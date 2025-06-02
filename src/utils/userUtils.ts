// utils/userUtils.ts
export const getInitials = (username: string | null | undefined): string => {
  if (!username) return '?';
  return username.substring(0, 2).toUpperCase();
};
export const getRankClass = (index: number): string => {
  if (index === 0) return 'rank-first';
  if (index === 1) return 'rank-second';
  if (index === 2) return 'rank-third';
  return '';
};