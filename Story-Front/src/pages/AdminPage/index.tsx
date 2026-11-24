import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { Button } from '../../components/UI/Button';
import { Notification } from '../../components/UI/Notification';
import {
  AccessPermission,
  AccessRole,
  AccessRoleRequest,
  adminApi,
  AdminApplication,
  AdminContest,
  AdminContestRequest,
  AdminUser,
  AdminWork,
} from '../../api/admin';
import { authApi } from '../../api/auth';
import { getRoleLabel } from '../../utils/roleLabels';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './index.module.scss';

type AdminTab = 'overview' | 'applications' | 'contests' | 'users' | 'access';

const emptyContestDraft: AdminContestRequest = {
  name: '',
  description: '',
  startAge: 0,
  endAge: 18,
};

const emptyRoleDraft: AccessRoleRequest = {
  name: '',
  description: '',
  permissions: [],
};

const getToken = () => localStorage.getItem('token');

const saveBlob = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
};

const formatDate = (value: string | undefined, locale: string) => {
  if (!value) return '—';
  const date = new Date(value.replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString(locale);
};

const formatSize = (bytes: number, language: 'ru' | 'en') => {
  if (bytes < 1024) return `${bytes} ${language === 'en' ? 'B' : 'Б'}`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} ${language === 'en' ? 'KB' : 'КБ'}`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} ${language === 'en' ? 'MB' : 'МБ'}`;
};

export const AdminPage = () => {
  const navigate = useNavigate();
  const { signOut } = useAuth();
  const { t, language } = useLanguage();
  const locale = language === 'en' ? 'en-US' : 'ru-RU';
  const [activeTab, setActiveTab] = useState<AdminTab>('overview');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [applications, setApplications] = useState<AdminApplication[]>([]);
  const [contests, setContests] = useState<AdminContest[]>([]);
  const [roles, setRoles] = useState<AccessRole[]>([]);
  const [permissions, setPermissions] = useState<AccessPermission[]>([]);
  const [userCount, setUserCount] = useState(0);
  const [applicationCount, setApplicationCount] = useState(0);
  const [eventClosed, setEventClosed] = useState(false);
  const [search, setSearch] = useState('');
  const [adminPassword, setAdminPassword] = useState('');
  const [contestDraft, setContestDraft] = useState<AdminContestRequest>(emptyContestDraft);
  const [editingContestId, setEditingContestId] = useState<number | null>(null);
  const [roleDraft, setRoleDraft] = useState<AccessRoleRequest>(emptyRoleDraft);
  const [editingRoleName, setEditingRoleName] = useState<string | null>(null);
  const [expandedApplicationId, setExpandedApplicationId] = useState<number | null>(null);
  const [worksByApplication, setWorksByApplication] = useState<Record<number, AdminWork[]>>({});
  const [ratingDrafts, setRatingDrafts] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loggingOut, setLoggingOut] = useState(false);

  const handleLogout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    const token = getToken();
    try {
      if (token) await authApi.logout(token);
    } finally {
      signOut();
      navigate('/', { replace: true });
    }
  };

  const loadDashboard = useCallback(async () => {
    const token = getToken();
    if (!token) {
      setError(t('Сессия администратора отсутствует. Войдите снова.'));
      setLoading(false);
      return;
    }

    setLoading(true);
    setError('');
    try {
      const results = await Promise.all([
        adminApi.getUsersCount(token),
        adminApi.getApplicationsCount(token),
        adminApi.getEventStatus(token),
        adminApi.getUsers(token),
        adminApi.getApplications(token),
        adminApi.getContests(token),
        adminApi.getRoles(token),
        adminApi.getPermissions(token),
      ]);
      const failed = results.find(result => result.error || result.status >= 400);
      if (failed) {
        throw new Error(failed.error?.message || t('Не удалось загрузить данные панели'));
      }

      const [
        usersTotal,
        applicationsTotal,
        event,
        usersResult,
        applicationsResult,
        contestsResult,
        rolesResult,
        permissionsResult,
      ] = results;
      setUserCount(usersTotal.data ?? 0);
      setApplicationCount(applicationsTotal.data ?? 0);
      setEventClosed(Boolean(event.data?.close));
      setUsers(usersResult.data ?? []);
      setApplications(applicationsResult.data ?? []);
      setContests(contestsResult.data ?? []);
      setRoles(rolesResult.data ?? []);
      setPermissions(permissionsResult.data ?? []);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : t('Ошибка загрузки панели администратора'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  const loadUsers = async (email = '') => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    setError('');
    try {
      const result = await adminApi.getUsers(token, email.trim() || undefined);
      if (result.error || result.status >= 400) {
        throw new Error(result.error?.message || t('Не удалось загрузить пользователей'));
      }
      setUsers(result.data ?? []);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : t('Ошибка загрузки пользователей'));
    } finally {
      setBusy(false);
    }
  };

  const requireAdminPassword = () => {
    if (adminPassword) return adminPassword;
    setError(t('Введите текущий пароль администратора для изменения доступа.'));
    return null;
  };

  const editContest = (contest: AdminContest) => {
    setEditingContestId(contest.conId);
    setContestDraft({
      name: contest.name,
      description: contest.description,
      startAge: contest.startAge,
      endAge: contest.endAge,
    });
  };

  const resetContestDraft = () => {
    setEditingContestId(null);
    setContestDraft(emptyContestDraft);
  };

  const saveContest = async (event: React.FormEvent) => {
    event.preventDefault();
    const token = getToken();
    if (!token || busy) return;
    if (!contestDraft.name.trim() || !contestDraft.description.trim()) {
      setError(t('Заполните название и описание номинации.'));
      return;
    }
    if (contestDraft.startAge < 0 || contestDraft.endAge < contestDraft.startAge) {
      setError(t('Проверьте возрастные ограничения номинации.'));
      return;
    }

    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = editingContestId === null
        ? await adminApi.createContest(contestDraft, token)
        : await adminApi.updateContest(editingContestId, contestDraft, token);
      if (result.error || !result.data) {
        throw new Error(result.error?.message || t('Не удалось сохранить номинацию'));
      }
      setContests(previous => editingContestId === null
        ? [...previous, result.data!]
        : previous.map(item => item.conId === editingContestId ? result.data! : item));
      setNotice(editingContestId === null ? t('Номинация создана.') : t('Номинация обновлена.'));
      resetContestDraft();
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось сохранить номинацию'));
    } finally {
      setBusy(false);
    }
  };

  const deleteContest = async (contest: AdminContest) => {
    const token = getToken();
    if (!token || busy || !window.confirm(t(`Удалить номинацию «${contest.name}»?`))) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await adminApi.deleteContest(contest.conId, token);
      if (result.error || result.status >= 400) {
        throw new Error(result.error?.message || t('Не удалось удалить номинацию'));
      }
      setContests(previous => previous.filter(item => item.conId !== contest.conId));
      if (editingContestId === contest.conId) resetContestDraft();
      setNotice(t('Номинация удалена.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось удалить номинацию'));
    } finally {
      setBusy(false);
    }
  };

  const editRole = (role: AccessRole) => {
    setEditingRoleName(role.name);
    setRoleDraft({
      name: role.name,
      description: role.description,
      permissions: [...role.permissions],
    });
  };

  const resetRoleDraft = () => {
    setEditingRoleName(null);
    setRoleDraft(emptyRoleDraft);
  };

  const togglePermission = (permissionName: string) => {
    setRoleDraft(previous => ({
      ...previous,
      permissions: previous.permissions.includes(permissionName)
        ? previous.permissions.filter(item => item !== permissionName)
        : [...previous.permissions, permissionName],
    }));
  };

  const saveRole = async (event: React.FormEvent) => {
    event.preventDefault();
    const token = getToken();
    if (!token || busy) return;
    if (!roleDraft.name.trim() || !roleDraft.description.trim() || roleDraft.permissions.length === 0) {
      setError(t('Укажите название, описание и хотя бы одно разрешение.'));
      return;
    }

    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = editingRoleName === null
        ? await adminApi.createRole(roleDraft, token)
        : await adminApi.updateRole(editingRoleName, roleDraft, token);
      if (result.error || !result.data) {
        throw new Error(result.error?.message || t('Не удалось сохранить роль'));
      }
      setRoles(previous => editingRoleName === null
        ? [...previous, result.data!]
        : previous.map(item => item.name === editingRoleName ? result.data! : item));
      setNotice(editingRoleName === null ? t('Роль создана.') : t('Роль обновлена.'));
      resetRoleDraft();
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось сохранить роль'));
    } finally {
      setBusy(false);
    }
  };

  const deleteRole = async (role: AccessRole) => {
    const token = getToken();
    if (!token || busy || role.system || !window.confirm(t(`Удалить роль «${role.name}»?`))) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await adminApi.deleteRole(role.name, token);
      if (result.error || result.status >= 400) {
        throw new Error(result.error?.message || t('Не удалось удалить роль'));
      }
      setRoles(previous => previous.filter(item => item.name !== role.name));
      if (editingRoleName === role.name) resetRoleDraft();
      setNotice(t('Роль удалена.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось удалить роль'));
    } finally {
      setBusy(false);
    }
  };

  const toggleUserBanned = async (user: AdminUser) => {
    const token = getToken();
    const currentPassword = requireAdminPassword();
    if (!token || !currentPassword || busy) return;

    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await adminApi.setUserBanned(user.userId, !user.banned, currentPassword, token);
      if (result.error || result.status >= 400) {
        throw new Error(result.error?.message || t('Не удалось изменить блокировку пользователя'));
      }
      setUsers(previous => previous.map(item => item.userId === user.userId
        ? { ...item, banned: !user.banned }
        : item));
      setAdminPassword('');
      setNotice(user.banned ? t('Пользователь разблокирован.') : t('Пользователь заблокирован.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось изменить блокировку пользователя'));
    } finally {
      setBusy(false);
    }
  };

  const changeUserRole = async (user: AdminUser, roleName: string) => {
    const token = getToken();
    const currentPassword = requireAdminPassword();
    if (!token || !currentPassword || busy) return;

    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await adminApi.setUserRoles(user.userId, [roleName], currentPassword, token);
      if (result.error || result.status >= 400) {
        throw new Error(result.error?.message || t('Не удалось изменить роль пользователя'));
      }
      setUsers(previous => previous.map(item => item.userId === user.userId
        ? { ...item, role: roleName, roles: [roleName] }
        : item));
      setAdminPassword('');
      setNotice(t('Роль пользователя обновлена.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось изменить роль пользователя'));
    } finally {
      setBusy(false);
    }
  };

  const toggleEvent = async () => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await adminApi.setEventStatus(!eventClosed, token);
      if (result.error || result.status >= 400) {
        throw new Error(result.error?.message || t('Не удалось изменить статус конкурса'));
      }
      setEventClosed(Boolean(result.data?.close));
      setNotice(result.data?.close ? t('Приём заявок закрыт.') : t('Приём заявок открыт.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Ошибка изменения статуса'));
    } finally {
      setBusy(false);
    }
  };

  const changeApplicationStatus = async (
    application: AdminApplication,
    status: AdminApplication['status'],
  ) => {
    const token = getToken();
    if (!token || busy) return;

    let rejectionReason: string | undefined;
    if (status === 'REJECTED') {
      const reason = window.prompt(
        t('Укажите причину отклонения'),
        application.rejectionReason || '',
      );
      if (reason === null) return;
      rejectionReason = reason.trim();
      if (!rejectionReason) {
        setError(t('Причина отклонения обязательна'));
        return;
      }
    }

    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await adminApi.setApplicationStatus(application.appId, status, rejectionReason, token);
      if (result.error || !result.data) {
        throw new Error(result.error?.message || t('Не удалось изменить статус заявки'));
      }
      setApplications(previous => previous.map(item => item.appId === application.appId ? result.data! : item));
      setNotice(t('Статус заявки обновлён.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось изменить статус заявки'));
    } finally {
      setBusy(false);
    }
  };

  const toggleWorks = async (applicationId: number) => {
    if (expandedApplicationId === applicationId) {
      setExpandedApplicationId(null);
      return;
    }
    setExpandedApplicationId(applicationId);
    if (worksByApplication[applicationId]) return;

    const token = getToken();
    if (!token) return;
    setBusy(true);
    setError('');
    try {
      const result = await adminApi.getWorks(applicationId, token);
      if (result.error || result.status >= 400) {
        throw new Error(result.error?.message || t('Не удалось загрузить файлы'));
      }
      const works = result.data ?? [];
      setWorksByApplication(previous => ({ ...previous, [applicationId]: works }));
      setRatingDrafts(previous => ({
        ...previous,
        ...Object.fromEntries(works.map(work => [work.workId, work.isRated ? String(work.rate) : ''])),
      }));
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : t('Ошибка загрузки файлов'));
    } finally {
      setBusy(false);
    }
  };

  const saveWorkRating = async (applicationId: number, work: AdminWork) => {
    const token = getToken();
    if (!token || busy) return;
    const rate = Number(ratingDrafts[work.workId]);
    if (!Number.isInteger(rate) || rate < 0 || rate > 100) {
      setError(t('Оценка должна быть целым числом от 0 до 100'));
      return;
    }

    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await adminApi.setWorkRating(work.workId, rate, token);
      if (result.error || !result.data) {
        throw new Error(result.error?.message || t('Не удалось сохранить оценку'));
      }
      setWorksByApplication(previous => ({
        ...previous,
        [applicationId]: (previous[applicationId] ?? []).map(item => item.workId === work.workId ? result.data! : item),
      }));
      setNotice(t('Оценка сохранена.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось сохранить оценку'));
    } finally {
      setBusy(false);
    }
  };

  const clearWorkRating = async (applicationId: number, work: AdminWork) => {
    const token = getToken();
    if (!token || busy) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await adminApi.clearWorkRating(work.workId, token);
      if (result.error || !result.data) {
        throw new Error(result.error?.message || t('Не удалось удалить оценку'));
      }
      setWorksByApplication(previous => ({
        ...previous,
        [applicationId]: (previous[applicationId] ?? []).map(item => item.workId === work.workId ? result.data! : item),
      }));
      setRatingDrafts(previous => ({ ...previous, [work.workId]: '' }));
      setNotice(t('Оценка удалена.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : t('Не удалось удалить оценку'));
    } finally {
      setBusy(false);
    }
  };

  const downloadFile = async (work: AdminWork) => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    setError('');
    try {
      const blob = await adminApi.downloadWork(work.workId, token);
      saveBlob(blob, work.name || `work-${work.workId}`);
    } catch (downloadError) {
      setError(downloadError instanceof Error ? downloadError.message : t('Ошибка скачивания файла'));
    } finally {
      setBusy(false);
    }
  };

  const downloadReport = async () => {
    const token = getToken();
    if (!token) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const blob = await adminApi.downloadApplicationsReport(token);
      saveBlob(blob, 'applications_report.xlsx');
      setNotice(t('Excel-отчёт скачан.'));
    } catch (downloadError) {
      setError(downloadError instanceof Error ? downloadError.message : t('Ошибка скачивания отчёта'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <DecorLines />
      <main className={css.page}>
        <section className={css.hero}>
          <div>
            <span className={css.eyebrow}>{t('УПРАВЛЕНИЕ КОНКУРСОМ')}</span>
            <h1 className={css.title}>{t('Панель администратора')}</h1>
            <p className={css.subtitle}>{t('Участники, заявки, работы и настройки приёма.')}</p>
          </div>
          <div className={css.heroActions}>
            <Button variant="secondary" onClick={downloadReport} disabled={busy}>{t('Скачать Excel-отчёт')}</Button>
            <Button variant="secondary" onClick={handleLogout} disabled={loggingOut}>
              {loggingOut ? t('Выход...') : t('Выйти')}
            </Button>
          </div>
        </section>

        {error && <div className={css.error} role="alert">{error}</div>}
        <section className={css.stats} aria-label={t('Сводка')}>
          <article className={css.statCard}>
            <span className={css.statLabel}>{t('Пользователи')}</span>
            <strong className={css.statValue}>{loading ? '…' : userCount}</strong>
          </article>
          <article className={css.statCard}>
            <span className={css.statLabel}>{t('Заявки')}</span>
            <strong className={css.statValue}>{loading ? '…' : applicationCount}</strong>
          </article>
          <article className={css.statCard}>
            <span className={css.statLabel}>{t('Приём заявок')}</span>
            <strong className={`${css.eventState} ${eventClosed ? css.closed : css.open}`}>
              {loading ? '…' : eventClosed ? t('Закрыт') : t('Открыт')}
            </strong>
            <button className={css.textButton} onClick={toggleEvent} disabled={loading || busy}>
              {eventClosed ? t('Открыть приём') : t('Закрыть приём')}
            </button>
          </article>
        </section>

        <nav className={css.tabs} aria-label={t('Разделы панели')}>
          <button className={activeTab === 'overview' ? css.activeTab : ''} onClick={() => setActiveTab('overview')}>{t('Обзор')}</button>
          <button className={activeTab === 'applications' ? css.activeTab : ''} onClick={() => setActiveTab('applications')}>{t('Заявки')}</button>
          <button className={activeTab === 'contests' ? css.activeTab : ''} onClick={() => setActiveTab('contests')}>{t('Номинации')}</button>
          <button className={activeTab === 'users' ? css.activeTab : ''} onClick={() => setActiveTab('users')}>{t('Участники')}</button>
          <button className={activeTab === 'access' ? css.activeTab : ''} onClick={() => setActiveTab('access')}>{t('Роли и доступ')}</button>
          <button className={css.refreshButton} onClick={() => void loadDashboard()} disabled={loading || busy}>{t('Обновить')}</button>
        </nav>

        {activeTab === 'overview' && (
          <section className={css.panel}>
            <div className={css.panelHeading}>
              <div><h2>{t('Последние заявки')}</h2><p>{t('Первая страница списка — до 100 заявок.')}</p></div>
              <button className={css.textButton} onClick={() => setActiveTab('applications')}>{t('Все заявки →')}</button>
            </div>
            {loading ? <p className={css.empty}>{t('Загрузка…')}</p> : applications.length === 0 ? (
              <p className={css.empty}>{t('Пока нет заявок.')}</p>
            ) : (
              <div className={css.recentList}>
                {applications.slice(0, 5).map(application => (
                  <article className={css.recentItem} key={application.appId}>
                    <div className={css.recentIcon}>З</div>
                    <div className={css.recentText}>
                      <strong>{application.name}</strong>
                      <span>{application.contestName} · {formatDate(application.createdAt, locale)}</span>
                    </div>
                    <span className={css.fileCount}>{application.worksCount} {t('файлов')}</span>
                  </article>
                ))}
              </div>
            )}
            <p className={css.capabilityNote}>{t('Заявки можно принимать, отклонять и возвращать на рассмотрение. Работам можно выставлять итоговую оценку от 0 до 100.')}</p>
          </section>
        )}

        {activeTab === 'applications' && (
          <section className={css.panel}>
            <div className={css.panelHeading}>
              <div><h2>{t('Заявки участников')}</h2><p>{t('Для заявки можно посмотреть работы и скачать файлы.')}</p></div>
              <span className={css.resultCount}>{applications.length} {t('из')} {applicationCount}</span>
            </div>
            {loading ? <p className={css.empty}>{t('Загрузка…')}</p> : applications.length === 0 ? (
              <p className={css.empty}>{t('Заявок пока нет.')}</p>
            ) : (
              <div className={css.applicationList}>
                {applications.map(application => (
                  <article className={css.applicationCard} key={application.appId}>
                    <div className={css.applicationTop}>
                      <div>
                        <h3>{application.name}</h3>
                        <span className={css.muted}>{application.contestName} · {formatDate(application.createdAt, locale)}</span>
                      </div>
                      <div className={css.applicationTopActions}>
                        <span className={`${css.statusBadge} ${css[`status${application.status}`]}`}>{t(
                          application.status === 'ACCEPTED' ? 'Принята' : application.status === 'REJECTED' ? 'Отклонена' : 'На рассмотрении'
                        )}</span>
                        <button className={css.outlineButton} onClick={() => void toggleWorks(application.appId)} disabled={busy}>
                          {expandedApplicationId === application.appId ? t('Скрыть работы') : `${t('Работы')} (${application.worksCount})`}
                        </button>
                      </div>
                    </div>
                    <div className={css.applicationMeta}>
                      <span>{t('Город:')} {application.city || '—'}</span>
                      <span>{t('Формат:')} {application.isTeam ? t('Команда') : t('Индивидуально')}</span>
                      {application.organization && <span>{t('Организация:')} {application.organization}</span>}
                      {application.mentorName && <span>{t('Наставник:')} {application.mentorName}</span>}
                    </div>
                    <p className={css.description}>{application.description}</p>
                    {application.rejectionReason && (
                      <p className={css.rejectionReason}><strong>{t('Причина отказа:')}</strong> {application.rejectionReason}</p>
                    )}
                    <div className={css.moderationActions}>
                      <button onClick={() => void changeApplicationStatus(application, 'ACCEPTED')} disabled={busy || application.status === 'ACCEPTED'}>{t('Принять')}</button>
                      <button onClick={() => void changeApplicationStatus(application, 'REJECTED')} disabled={busy || application.status === 'REJECTED'}>{t('Отклонить')}</button>
                      <button onClick={() => void changeApplicationStatus(application, 'PENDING')} disabled={busy || application.status === 'PENDING'}>{t('Вернуть на рассмотрение')}</button>
                    </div>
                    {expandedApplicationId === application.appId && (
                      <div className={css.worksList}>
                        {(worksByApplication[application.appId] ?? []).length === 0 ? (
                          <span className={css.muted}>{busy ? t('Загрузка файлов…') : t('В этой заявке нет файлов.')}</span>
                        ) : (worksByApplication[application.appId] ?? []).map(work => (
                          <div className={css.workItem} key={work.workId}>
                            <div>
                              <strong>{work.name}</strong>
                              <span>{formatSize(work.fileSize, language)} · {work.broken ? t('Файл повреждён') : t('Доступен')} · {work.isRated ? `${t('Оценка')}: ${work.rate}/100` : t('Без оценки')}</span>
                            </div>
                            <div className={css.workActions}>
                              <input
                                type="number"
                                min="0"
                                max="100"
                                step="1"
                                value={ratingDrafts[work.workId] ?? ''}
                                onChange={event => setRatingDrafts(previous => ({ ...previous, [work.workId]: event.target.value }))}
                                aria-label={t('Оценка от 0 до 100')}
                                disabled={busy || work.broken}
                              />
                              <button className={css.textButton} onClick={() => void saveWorkRating(application.appId, work)} disabled={busy || work.broken}>{t('Сохранить оценку')}</button>
                              {work.isRated && <button className={css.textButton} onClick={() => void clearWorkRating(application.appId, work)} disabled={busy}>{t('Снять оценку')}</button>}
                              <button className={css.textButton} onClick={() => void downloadFile(work)} disabled={busy || work.broken}>{t('Скачать')}</button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
          </section>
        )}

        {activeTab === 'contests' && (
          <section className={css.panel}>
            <div className={css.panelHeading}>
              <div>
                <h2>{t('Номинации')}</h2>
                <p>{t('Создание и изменение направлений конкурса и возрастных ограничений.')}</p>
              </div>
              <span className={css.resultCount}>{contests.length} {t('всего')}</span>
            </div>

            <form className={css.contestForm} onSubmit={saveContest}>
              <label>
                <span>{t('Название')}</span>
                <input
                  value={contestDraft.name}
                  onChange={event => setContestDraft(previous => ({ ...previous, name: event.target.value }))}
                  required
                />
              </label>
              <label className={css.contestDescription}>
                <span>{t('Описание')}</span>
                <textarea
                  value={contestDraft.description}
                  onChange={event => setContestDraft(previous => ({ ...previous, description: event.target.value }))}
                  required
                />
              </label>
              <label>
                <span>{t('Возраст от')}</span>
                <input
                  type="number"
                  min="0"
                  value={contestDraft.startAge}
                  onChange={event => setContestDraft(previous => ({ ...previous, startAge: Number(event.target.value) }))}
                  required
                />
              </label>
              <label>
                <span>{t('Возраст до')}</span>
                <input
                  type="number"
                  min={contestDraft.startAge}
                  value={contestDraft.endAge}
                  onChange={event => setContestDraft(previous => ({ ...previous, endAge: Number(event.target.value) }))}
                  required
                />
              </label>
              <div className={css.contestFormActions}>
                <Button variant="primary" type="submit" disabled={busy}>
                  {editingContestId === null ? t('Создать номинацию') : t('Сохранить изменения')}
                </Button>
                {editingContestId !== null && (
                  <button className={css.textButton} type="button" onClick={resetContestDraft} disabled={busy}>
                    {t('Отмена')}
                  </button>
                )}
              </div>
            </form>

            <div className={css.contestList}>
              {contests.length === 0 ? <p className={css.empty}>{t('Номинаций пока нет.')}</p> : contests.map(contest => (
                <article className={css.contestRow} key={contest.conId}>
                  <div>
                    <strong>{contest.name}</strong>
                    <span>{contest.description}</span>
                    <small>{t('Возраст:')} {contest.startAge}–{contest.endAge}</small>
                  </div>
                  <div className={css.userActions}>
                    <button className={css.textButton} onClick={() => editContest(contest)} disabled={busy}>{t('Редактировать')}</button>
                    <button className={css.textButton} onClick={() => void deleteContest(contest)} disabled={busy}>{t('Удалить')}</button>
                  </div>
                </article>
              ))}
            </div>
          </section>
        )}

        {activeTab === 'access' && (
          <section className={css.panel}>
            <div className={css.panelHeading}>
              <div>
                <h2>{t('Роли и доступ к ресурсам')}</h2>
                <p>{t('Permissions определяют, какие API-операции доступны пользователям роли.')}</p>
              </div>
              <span className={css.resultCount}>{roles.length} {t('ролей')}</span>
            </div>

            <form className={css.roleForm} onSubmit={saveRole}>
              <label>
                <span>{t('Название роли')}</span>
                <input
                  value={roleDraft.name}
                  onChange={event => setRoleDraft(previous => ({ ...previous, name: event.target.value.toUpperCase() }))}
                  disabled={editingRoleName !== null}
                  placeholder="CURATOR"
                  required
                />
              </label>
              <label>
                <span>{t('Описание')}</span>
                <input
                  value={roleDraft.description}
                  onChange={event => setRoleDraft(previous => ({ ...previous, description: event.target.value }))}
                  required
                />
              </label>
              <fieldset>
                <legend>{t('Разрешения для API-ресурсов')}</legend>
                <div className={css.permissionGrid}>
                  {permissions.map(permission => (
                    <label className={css.permissionOption} key={permission.name}>
                      <input
                        type="checkbox"
                        checked={roleDraft.permissions.includes(permission.name)}
                        onChange={() => togglePermission(permission.name)}
                      />
                      <span><strong>{permission.name}</strong><small>{permission.description}</small></span>
                    </label>
                  ))}
                </div>
              </fieldset>
              <div className={css.contestFormActions}>
                <Button variant="primary" type="submit" disabled={busy}>
                  {editingRoleName === null ? t('Создать роль') : t('Сохранить доступ')}
                </Button>
                {editingRoleName !== null && (
                  <button className={css.textButton} type="button" onClick={resetRoleDraft} disabled={busy}>{t('Отмена')}</button>
                )}
              </div>
            </form>

            <div className={css.roleList}>
              {roles.map(role => (
                <article className={css.roleRow} key={role.name}>
                  <div>
                    <strong>{role.name}</strong>
                    <span>{role.description}</span>
                    <small>{role.assignedUsers} {t('пользователей')} · {role.permissions.length} {t('разрешений')}</small>
                  </div>
                  <div className={css.userActions}>
                    <button className={css.textButton} onClick={() => editRole(role)} disabled={busy}>{t('Настроить доступ')}</button>
                    {!role.system && (
                      <button className={css.textButton} onClick={() => void deleteRole(role)} disabled={busy || role.assignedUsers > 0}>{t('Удалить')}</button>
                    )}
                  </div>
                </article>
              ))}
            </div>
          </section>
        )}

        {activeTab === 'users' && (
          <section className={css.panel}>
            <div className={css.panelHeading}>
              <div><h2>{t('Участники')}</h2><p>{t('Поиск по email, роль и количество заявок.')}</p></div>
              <span className={css.resultCount}>{users.length} {t('показано')} · {userCount} {t('всего')}</span>
            </div>
            <form className={css.search} onSubmit={event => { event.preventDefault(); void loadUsers(search); }}>
              <input value={search} onChange={event => setSearch(event.target.value)} placeholder={t('Поиск по email')} aria-label={t('Поиск по email')} />
              <Button variant="primary" type="submit" disabled={busy}>{t('Найти')}</Button>
              {search && <button className={css.textButton} type="button" onClick={() => { setSearch(''); void loadUsers(); }}>{t('Сбросить')}</button>}
            </form>
            <div className={css.securityPanel}>
              <label>
                <span>{t('Текущий пароль администратора')}</span>
                <input
                  type="password"
                  value={adminPassword}
                  onChange={event => setAdminPassword(event.target.value)}
                  autoComplete="current-password"
                  placeholder={t('Нужен для роли и блокировки')}
                />
              </label>
              <p>{t('Пароль не сохраняется и очищается после успешного изменения.')}</p>
            </div>
            {loading ? <p className={css.empty}>{t('Загрузка…')}</p> : users.length === 0 ? (
              <p className={css.empty}>{t('Пользователи не найдены.')}</p>
            ) : (
              <div className={css.userList}>
                {users.map(user => (
                  <article className={css.userRow} key={user.userId}>
                    <div className={css.avatar}>{`${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}` || 'У'}</div>
                    <div className={css.userIdentity}>
                      <strong>{user.firstName} {user.lastName}</strong>
                      <span>{user.email}</span>
                    </div>
                    <span className={`${css.roleBadge} ${user.role === 'ADMIN' ? css.adminBadge : ''}`}>{t(getRoleLabel(user.role))}</span>
                    {user.banned && <span className={css.bannedBadge}>{t('Заблокирован')}</span>}
                    <span className={css.userApplications}>{user.applicationsCount} {t('заявок')}</span>
                    <div className={css.userActions}>
                      <button className={css.textButton} onClick={() => void toggleUserBanned(user)} disabled={busy}>
                        {user.banned ? t('Разблокировать') : t('Заблокировать')}
                      </button>
                      <select
                        value={user.role}
                        onChange={event => void changeUserRole(user, event.target.value)}
                        disabled={busy}
                        aria-label={t('Роль пользователя')}
                      >
                        {roles.map(role => <option key={role.name} value={role.name}>{role.name}</option>)}
                      </select>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        )}
      </main>
      <Notification
        message={notice}
        type="success"
        isVisible={Boolean(notice)}
        onClose={() => setNotice('')}
      />
    </>
  );
};
