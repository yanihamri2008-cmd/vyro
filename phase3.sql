-- VYRO phase 3: messaging, groups, reports, admin + bans. Run once in the Supabase SQL editor.

-- ===== reports: add status =====
alter table public.reports add column if not exists status text not null default 'open'
  check (status in ('open','resolved','dismissed'));
alter table public.reports add column if not exists handled_by uuid references public.profiles(id) on delete set null;
alter table public.reports add column if not exists handled_at timestamptz;

-- ===== admin + bans (server-side only) =====
create table if not exists public.admins (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now()
);
create table if not exists public.bans (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  reason text,
  banned_by uuid references public.profiles(id) on delete set null,
  banned_at timestamptz not null default now(),
  expires_at timestamptz
);
create table if not exists public.admin_actions (
  id uuid primary key default gen_random_uuid(),
  admin_id uuid references public.profiles(id) on delete set null,
  action text not null,
  target_user uuid,
  target_id uuid,
  note text,
  created_at timestamptz not null default now()
);
alter table public.admins enable row level security;
alter table public.bans enable row level security;
alter table public.admin_actions enable row level security;

create or replace function public.is_admin(u uuid default auth.uid()) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from admins where user_id = u);
$$;
create or replace function public.is_banned(u uuid default auth.uid()) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from bans where user_id = u and (expires_at is null or expires_at > now()));
$$;
create or replace function public.require_admin() returns void
language plpgsql stable security definer set search_path = public as $$
begin if not is_admin() then raise exception 'forbidden'; end if; end $$;

create policy "admins read actions" on public.admin_actions for select to authenticated using (public.is_admin());

create or replace function public.am_i_admin() returns boolean
language sql stable security definer set search_path = public as $$ select is_admin(); $$;

create or replace function public.my_ban() returns table (reason text, expires_at timestamptz)
language sql stable security definer set search_path = public as $$
  select b.reason, b.expires_at from bans b
  where b.user_id = auth.uid() and (b.expires_at is null or b.expires_at > now());
$$;

-- hide banned users' content from everyone except themselves and admins
create or replace function public.can_view(author uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select
    not exists (select 1 from blocks b where (b.blocker_id = author and b.blocked_id = auth.uid())
                                          or (b.blocker_id = auth.uid() and b.blocked_id = author))
    and (author = auth.uid() or is_admin() or not is_banned(author))
    and (
      author = auth.uid()
      or not (select is_private from profiles where id = author)
      or exists (select 1 from follows f where f.follower_id = auth.uid()
                 and f.followee_id = author and f.status = 'accepted')
    );
$$;

-- banned users cannot create anything
drop policy if exists "post as me" on public.posts;
create policy "post as me" on public.posts for insert to authenticated
  with check (author_id = auth.uid() and not public.is_banned());
drop policy if exists "comment as me" on public.comments;
create policy "comment as me" on public.comments for insert to authenticated
  with check (author_id = auth.uid() and not public.is_banned()
              and exists (select 1 from posts p where p.id = post_id and public.can_view(p.author_id)));
drop policy if exists "like as me" on public.likes;
create policy "like as me" on public.likes for insert to authenticated
  with check (user_id = auth.uid() and not public.is_banned());
drop policy if exists "follow as me" on public.follows;
create policy "follow as me" on public.follows for insert to authenticated
  with check (follower_id = auth.uid() and not public.is_banned());
drop policy if exists "upload to own folder" on storage.objects;
create policy "upload to own folder" on storage.objects for insert to authenticated
  with check (bucket_id in ('avatars','posts') and (storage.foldername(name))[1] = auth.uid()::text
              and not public.is_banned());

-- ===== admin actions (all check is_admin) =====
create or replace function public.admin_reports()
returns table (id uuid, reporter text, target_type text, target_id uuid, preview text,
               target_user uuid, target_username text, reason text, created_at timestamptz)
language plpgsql stable security definer set search_path = public as $$
begin
  perform require_admin();
  return query
  select r.id, rp.username, r.target_type, r.target_id,
         case r.target_type
           when 'post' then (select left(coalesce(po.caption,''),120) from posts po where po.id = r.target_id)
           when 'comment' then (select left(co.body,120) from comments co where co.id = r.target_id)
           else null end,
         tu.uid, (select pu.username from profiles pu where pu.id = tu.uid), r.reason, r.created_at
  from reports r
  join profiles rp on rp.id = r.reporter_id
  cross join lateral (
    select case r.target_type
             when 'post' then (select po2.author_id from posts po2 where po2.id = r.target_id)
             when 'comment' then (select co2.author_id from comments co2 where co2.id = r.target_id)
             else r.target_id end as uid) tu
  where r.status = 'open'
  order by r.created_at desc limit 100;
end $$;

create or replace function public.admin_users(q text default '')
returns table (id uuid, username text, display_name text, banned boolean)
language plpgsql stable security definer set search_path = public as $$
begin
  perform require_admin();
  return query
  select p.id, p.username, p.display_name, is_banned(p.id)
  from profiles p
  where (q is null or q = '' or p.username ilike '%' || q || '%')
  order by p.created_at desc limit 40;
end $$;

create or replace function public.admin_ban(p_user uuid, p_reason text, p_days int default null) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform require_admin();
  if p_user = auth.uid() or is_admin(p_user) then raise exception 'cannot ban an admin'; end if;
  insert into bans (user_id, reason, banned_by, expires_at)
  values (p_user, nullif(trim(p_reason),''), auth.uid(),
          case when p_days is null then null else now() + make_interval(days => p_days) end)
  on conflict (user_id) do update
    set reason = excluded.reason, banned_by = excluded.banned_by, banned_at = now(), expires_at = excluded.expires_at;
  insert into admin_actions (admin_id, action, target_user, note) values (auth.uid(), 'ban', p_user, p_reason);
end $$;

create or replace function public.admin_unban(p_user uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform require_admin();
  delete from bans where user_id = p_user;
  insert into admin_actions (admin_id, action, target_user) values (auth.uid(), 'unban', p_user);
end $$;

create or replace function public.admin_delete_post(p_post uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform require_admin();
  insert into admin_actions (admin_id, action, target_id, target_user)
    select auth.uid(), 'delete_post', p_post, author_id from posts where id = p_post;
  delete from posts where id = p_post;
end $$;

create or replace function public.admin_delete_comment(p_comment uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform require_admin();
  insert into admin_actions (admin_id, action, target_id, target_user)
    select auth.uid(), 'delete_comment', p_comment, author_id from comments where id = p_comment;
  delete from comments where id = p_comment;
end $$;

create or replace function public.admin_resolve_report(p_report uuid, p_status text) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform require_admin();
  if p_status not in ('resolved','dismissed') then raise exception 'bad status'; end if;
  update reports set status = p_status, handled_by = auth.uid(), handled_at = now() where id = p_report;
end $$;

-- ===== messaging =====
create table public.conversations (
  id uuid primary key default gen_random_uuid(),
  kind text not null check (kind in ('dm','group')),
  title text,
  avatar_url text,
  created_by uuid references public.profiles(id) on delete set null,
  created_at timestamptz not null default now(),
  last_message_at timestamptz not null default now()
);
create table public.conversation_members (
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('owner','admin','member')),
  joined_at timestamptz not null default now(),
  last_read_at timestamptz not null default now(),
  primary key (conversation_id, user_id)
);
create index cm_user on public.conversation_members(user_id);
create table public.messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 4000),
  created_at timestamptz not null default now(),
  edited_at timestamptz,
  deleted boolean not null default false
);
create index msg_conv on public.messages(conversation_id, created_at desc);

create or replace function public.is_member(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from conversation_members where conversation_id = cid and user_id = auth.uid());
$$;
create or replace function public.is_conv_admin(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from conversation_members
                 where conversation_id = cid and user_id = auth.uid() and role in ('owner','admin'));
$$;
create or replace function public.can_message(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select is_member(cid) and not is_banned()
  and not exists (
    select 1 from conversations c
    join conversation_members o on o.conversation_id = c.id and o.user_id <> auth.uid()
    join blocks b on (b.blocker_id = auth.uid() and b.blocked_id = o.user_id)
                  or (b.blocker_id = o.user_id and b.blocked_id = auth.uid())
    where c.id = cid and c.kind = 'dm');
$$;

alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;

create policy "see my conversations" on public.conversations for select to authenticated using (public.is_member(id));
create policy "see members of my conversations" on public.conversation_members for select to authenticated
  using (public.is_member(conversation_id));
create policy "leave or remove" on public.conversation_members for delete to authenticated
  using (user_id = auth.uid() or public.is_conv_admin(conversation_id));
create policy "read messages" on public.messages for select to authenticated using (public.is_member(conversation_id));
create policy "send messages" on public.messages for insert to authenticated
  with check (sender_id = auth.uid() and public.can_message(conversation_id));
create policy "edit own messages" on public.messages for update to authenticated
  using (sender_id = auth.uid()) with check (sender_id = auth.uid());

create or replace function public.touch_conversation() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  update conversations set last_message_at = new.created_at where id = new.conversation_id;
  return new;
end $$;
create trigger t_msg_touch after insert on public.messages
  for each row execute function public.touch_conversation();

create or replace function public.start_dm(other uuid) returns uuid
language plpgsql security definer set search_path = public as $$
declare cid uuid; me uuid := auth.uid();
begin
  if me is null or other is null or other = me then raise exception 'invalid'; end if;
  if is_banned(me) then raise exception 'banned'; end if;
  if exists (select 1 from blocks b where (b.blocker_id = me and b.blocked_id = other)
                                       or (b.blocker_id = other and b.blocked_id = me)) then
    raise exception 'blocked';
  end if;
  select c.id into cid from conversations c
  where c.kind = 'dm'
    and exists (select 1 from conversation_members a where a.conversation_id = c.id and a.user_id = me)
    and exists (select 1 from conversation_members b2 where b2.conversation_id = c.id and b2.user_id = other)
  limit 1;
  if cid is null then
    insert into conversations (kind, created_by) values ('dm', me) returning id into cid;
    insert into conversation_members (conversation_id, user_id) values (cid, me), (cid, other);
  end if;
  return cid;
end $$;

create or replace function public.create_group(p_title text, p_members uuid[]) returns uuid
language plpgsql security definer set search_path = public as $$
declare cid uuid; me uuid := auth.uid();
begin
  if me is null or is_banned(me) then raise exception 'not allowed'; end if;
  if char_length(trim(coalesce(p_title,''))) = 0 then raise exception 'title required'; end if;
  if coalesce(array_length(p_members,1),0) > 100 then raise exception 'too many members'; end if;
  insert into conversations (kind, title, created_by) values ('group', trim(p_title), me) returning id into cid;
  insert into conversation_members (conversation_id, user_id, role) values (cid, me, 'owner');
  insert into conversation_members (conversation_id, user_id)
    select cid, p.id from profiles p
    where p.id = any(p_members) and p.id <> me
      and not exists (select 1 from blocks b where (b.blocker_id = me and b.blocked_id = p.id)
                                                or (b.blocker_id = p.id and b.blocked_id = me))
    on conflict do nothing;
  return cid;
end $$;

create or replace function public.add_group_members(p_cid uuid, p_members uuid[]) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not is_conv_admin(p_cid) then raise exception 'forbidden'; end if;
  if not exists (select 1 from conversations where id = p_cid and kind = 'group') then raise exception 'not a group'; end if;
  if (select count(*) from conversation_members where conversation_id = p_cid) + coalesce(array_length(p_members,1),0) > 200 then
    raise exception 'group too large';
  end if;
  insert into conversation_members (conversation_id, user_id)
    select p_cid, p.id from profiles p where p.id = any(p_members) on conflict do nothing;
end $$;

create or replace function public.mark_read(p_cid uuid) returns void
language sql security definer set search_path = public as $$
  update conversation_members set last_read_at = now() where conversation_id = p_cid and user_id = auth.uid();
$$;

create or replace function public.get_inbox()
returns table (id uuid, kind text, title text, avatar_url text, last_body text, last_at timestamptz,
               unread bigint, peer_id uuid, peer_username text, peer_avatar text)
language sql stable security invoker set search_path = public as $$
  select c.id, c.kind, c.title, c.avatar_url,
         (select m.body from messages m where m.conversation_id = c.id and not m.deleted order by m.created_at desc limit 1),
         c.last_message_at,
         (select count(*) from messages m where m.conversation_id = c.id and m.sender_id <> auth.uid()
                 and m.created_at > cm.last_read_at and not m.deleted),
         pe.pid, pe.pname, pe.pavatar
  from conversation_members cm
  join conversations c on c.id = cm.conversation_id
  left join lateral (
    select pr.id as pid, pr.username as pname, pr.avatar_url as pavatar
    from conversation_members o join profiles pr on pr.id = o.user_id
    where o.conversation_id = c.id and o.user_id <> auth.uid() and c.kind = 'dm' limit 1) pe on true
  where cm.user_id = auth.uid()
  order by c.last_message_at desc;
$$;

alter publication supabase_realtime add table public.messages;

-- ===== make yourself the first admin (replace YOUR_USERNAME, run separately) =====
-- insert into public.admins (user_id) select id from public.profiles where username = 'YOUR_USERNAME';
