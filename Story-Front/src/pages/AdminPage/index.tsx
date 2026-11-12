import React, { useCallback, useEffect, useState } from 'react';
import { DecorLines } from '../../components/Layout/DecorLines';
import { Button } from '../../components/UI/Button';
import { adminApi, AdminApplication, AdminUser, AdminWork } from '../../api/admin';
import { getRoleLabel } from '../../utils/roleLabels';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './index.module.scss';

type AdminTab = 'overview' | 'applications' | 'users';

const getToken = () => localStorage.getItem('token');

const saveBlob = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
};

const formatDate = (value?: string) => {
  if (!value) return '—';
  const date = new Date(value.replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ru-RU');
};

const formatSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} Б`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} КБ`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} МБ`;
};

export const AdminPage = () => {
  const { t, language } = useLanguage();
  const [activeTab, setActiveTab] = useState<AdminTab>('overview');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [applications, setApplications] = useState<AdminApplication[]>([]);
  const [userCount, setUserCount] = useState(0);
  const [applicationCount, setApplicationCount] = useState(0);
  const [eventClosed, setEventClosed] = useState(false);
  const [search, setSearch] = useState('');
  const [expandedApplicationId, setExpandedApplicationId] = useState<number | null>(null);
  const [worksByApplication, setWorksByApplication] = useState<Record<number, AdminWork[]>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

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
      ]);
      const failed = results.find(result => result.error || result.status >= 400);
      if (failed) throw new Error(failed.error?.message || t('Не удалось загрузить данные панели'));

      const [usersTotal, applicationsTotal, event, usersResult, applicationsResult] = results;
      setUserCount(usersTotal.data ?? 0);
      setApplicationCount(applicationsTotal.data ?? 0);
      setEventClosed(Boolean(event.data?.close));
      setUsers(usersResult.data ?? []);
      setApplications(applicationsResult.data ?? []);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Ошибка загрузки панели администратора');
    } finally {
      setLoading(false);
    }
  }, []);

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
      if (result.error || result.status >= 400) throw new Error(result.error?.message || t('Не удалось загрузить пользователей'));
      setUsers(result.data ?? []);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Ошибка загрузки пользователей');
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
      if (result.error || result.status >= 400) throw new Error(result.error?.message || t('Не удалось изменить статус конкурса'));
      setEventClosed(Boolean(result.data?.close));
      setNotice(result.data?.close ? t('Приём заявок закрыт.') : t('Приём заявок открыт.'));
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : 'Ошибка изменения статуса');
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
      if (result.error || result.status >= 400) throw new Error(result.error?.message || t('Не удалось загрузить файлы'));
      setWorksByApplication(previous => ({ ...previous, [applicationId]: result.data ?? [] }));
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Ошибка загрузки файлов');
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
      saveBlob(await adminApi.downloadWork(work.workId, token), work.name || `work-${work.workId}`);
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
      saveBlob(await adminApi.downloadApplicationsReport(token), 'applications_report.xlsx');
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
          <Button variant="secondary" onClick={downloadReport} disabled={busy}>{t('Скачать Excel-отчёт')}</Button>
        </section>

        {error && <div className={css.error} role="alert">{error}</div>}
        {notice && <div className={css.notice} role="status">{notice}</div>}

        <section className={css.stats} aria-label="Сводка">
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
              {loading ? '…' : eventClosed ? 'Закрыт' : 'Открыт'}
            </strong>
            <button className={css.textButton} onClick={toggleEvent} disabled={loading || busy}>
              {eventClosed ? t('Открыть приём') : t('Закрыть приём')}
            </button>
          </article>
        </section>

        <nav className={css.tabs} aria-label="Разделы панели">
          <button className={activeTab === 'overview' ? css.activeTab : ''} onClick={() => setActiveTab('overview')}>{t('Обзор')}</button>
          <button className={activeTab === 'applications' ? css.activeTab : ''} onClick={() => setActiveTab('applications')}>{t('Заявки')}</button>
          <button className={activeTab === 'users' ? css.activeTab : ''} onClick={() => setActiveTab('users')}>{t('Участники')}</button>
          <button className={css.refreshButton} onClick={loadDashboard} disabled={loading || busy}>{t('Обновить')}</button>
        </nav>

        {activeTab === 'overview' && (
          <section className={css.panel}>
            <div className={css.panelHeading}>
              <div><h2>{t('Последние заявки')}</h2><p>{t('Первая страница списка — до 100 заявок.')}</p></div>
              <button className={css.textButton} onClick={() => setActiveTab('applications')}>{t('Все заявки →')}</button>
            </div>
            {loading ? <p className={css.empty}>Загрузка…</p> : applications.length === 0 ? (
              <p className={css.empty}>Пока нет заявок.</p>
            ) : (
              <div className={css.recentList}>
                {applications.slice(0, 5).map(application => (
                  <article className={css.recentItem} key={application.appId}>
                    <div className={css.recentIcon}>З</div>
                    <div className={css.recentText}>
                      <strong>{application.name}</strong>
                      <span>{application.contestName} · {formatDate(application.createdAt)}</span>
                    </div>
                    <span className={css.fileCount}>{application.worksCount} файлов</span>
                  </article>
                ))}
              </div>
            )}
            <p className={css.capabilityNote}>Доступны просмотр заявок и работ, выгрузка Excel и управление приёмом. Оценки и смены статуса заявок в backend пока нет.</p>
          </section>
        )}

        {activeTab === 'applications' && (
          <section className={css.panel}>
            <div className={css.panelHeading}>
              <div><h2>{t('Заявки участников')}</h2><p>{t('Для заявки можно посмотреть работы и скачать файлы.')}</p></div>
              <span className={css.resultCount}>{applications.length} {t('из')} {applicationCount}</span>
            </div>
            {loading ? <p className={css.empty}>Загрузка…</p> : applications.length === 0 ? (
              <p className={css.empty}>Заявок пока нет.</p>
            ) : (
              <div className={css.applicationList}>
                {applications.map(application => (
                  <article className={css.applicationCard} key={application.appId}>
                    <div className={css.applicationTop}>
                      <div><h3>{application.name}</h3><span className={css.muted}>{application.contestName} · {formatDate(application.createdAt)}</span></div>
                      <button className={css.outlineButton} onClick={() => toggleWorks(application.appId)} disabled={busy}>
                        {expandedApplicationId === application.appId ? t('Скрыть работы') : `${t('Работы')} (${application.worksCount})`}
                      </button>
                    </div>
                    <div className={css.applicationMeta}>
                      <span>Город: {application.city || '—'}</span>
                      <span>Формат: {application.isTeam ? 'Команда' : 'Индивидуально'}</span>
                      {application.organization && <span>Организация: {application.organization}</span>}
                      {application.mentorName && <span>Наставник: {application.mentorName}</span>}
                    </div>
                    <p className={css.description}>{application.description}</p>
                    {expandedApplicationId === application.appId && (
                      <div className={css.worksList}>
                        {(worksByApplication[application.appId] ?? []).length === 0 ? (
                          <span className={css.muted}>{busy ? t('Загрузка файлов…') : t('В этой заявке нет файлов.')}</span>
                        ) : (worksByApplication[application.appId] ?? []).map(work => (
                          <div className={css.workItem} key={work.workId}>
                            <div><strong>{work.name}</strong><span>{formatSize(work.fileSize)} · {work.broken ? 'Файл повреждён' : 'Доступен'}</span></div>
                            <button className={css.textButton} onClick={() => downloadFile(work)} disabled={busy || work.broken}>{t('Скачать')}</button>
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

        {activeTab === 'users' && (
          <section className={css.panel}>
            <div className={css.panelHeading}>
              <div><h2>{t('Участники')}</h2><p>{t('Поиск по email, роль и количество заявок.')}</p></div>
              <span className={css.resultCount}>{users.length} {t('показано')} · {userCount} {t('всего')}</span>
            </div>
            <form className={css.search} onSubmit={(event) => { event.preventDefault(); void loadUsers(search); }}>
              <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder={t('Поиск по email')} aria-label={t('Поиск по email')} />
              <Button variant="primary" type="submit" disabled={busy}>{t('Найти')}</Button>
              {search && <button className={css.textButton} type="button" onClick={() => { setSearch(''); void loadUsers(); }}>{t('Сбросить')}</button>}
            </form>
            {loading ? <p className={css.empty}>Загрузка…</p> : users.length === 0 ? (
              <p className={css.empty}>Пользователи не найдены.</p>
            ) : (
              <div className={css.userList}>
                {users.map(user => (
                  <article className={css.userRow} key={user.userId}>
                    <div className={css.avatar}>{`${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}` || 'У'}</div>
                    <div className={css.userIdentity}><strong>{user.firstName} {user.lastName}</strong><span>{user.email}</span></div>
                    <span className={`${css.roleBadge} ${user.role === 'ADMIN' ? css.adminBadge : ''}`}>{t(getRoleLabel(user.role))}</span>
                    <span className={css.userApplications}>{user.applicationsCount} {t('заявок')}</span>
                  </article>
                ))}
              </div>
            )}
          </section>
        )}
      </main>
    </>
  );
};
