export const adminPermissions = [
  'user:manage', 'role:manage', 'application:read:any', 'application:moderate',
  'work:read:any', 'work:rate', 'event:manage', 'report:export', 'contest:manage',
];

export const canOpenAdmin = (permissions: string[]) =>
  adminPermissions.some(permission => permissions.includes(permission));

export const landingPath = (permissions: string[]) =>
  canOpenAdmin(permissions) ? '/admin' : '/profile';
