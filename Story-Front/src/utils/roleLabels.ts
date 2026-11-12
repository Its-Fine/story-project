export const getRoleLabel = (role?: string | null): string => {
  switch (role) {
    case 'PARTICIPANT':
      return 'Участник';
    case 'ADMIN':
      return 'Администратор';
    default:
      return role ?? '';
  }
};
