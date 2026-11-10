-- VYRO phase 2a: profile stats + safe follow status. Run once in the Supabase SQL editor.
create or replace function public.get_profile(p_user uuid)
returns table (id uuid, username text, display_name text, bio text, avatar_url text, is_private boolean,
               posts_count bigint, followers_count bigint, following_count bigint, follow_status text)
language sql stable security invoker set search_path = public as $$
  select p.id, p.username, p.display_name, p.bio, p.avatar_url, p.is_private,
         (select count(*) from posts x where x.author_id = p.id),
         (select count(*) from follows f where f.followee_id = p.id and f.status = 'accepted'),
         (select count(*) from follows f where f.follower_id = p.id and f.status = 'accepted'),
         (select f.status from follows f where f.follower_id = auth.uid() and f.followee_id = p.id)
  from profiles p where p.id = p_user;
$$;

create or replace function public.enforce_follow_status() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if (select is_private from profiles where id = new.followee_id) then new.status := 'pending';
  else new.status := 'accepted';
  end if;
  return new;
end $$;
drop trigger if exists t_follow_status on public.follows;
create trigger t_follow_status before insert on public.follows
  for each row execute function public.enforce_follow_status();
