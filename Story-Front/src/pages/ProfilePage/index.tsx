import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { Button } from '../../components/UI/Button';
import { applicationsApi } from '../../api/applications';
import { usersApi, AccountData } from '../../api/users';
import { API_BASE_URL } from '../../api/client';
import { Application, Contest } from '../../api/applications';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './index.module.scss';

export const ProfilePage = () => {
  const navigate = useNavigate();
  const { role, signOut } = useAuth();
  const { t } = useLanguage();
  const [account, setAccount] = useState<AccountData | null>(null);
  const [applications, setApplications] = useState<Application[]>([]);
  const [contests, setContests] = useState<Contest[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [profileDraft, setProfileDraft] = useState({ firstName: '', lastName: '' });

  const fetchApplications = useCallback(async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/', { replace: true });
      return;
    }

    const response = await applicationsApi.getUserApplications(token);
    if (response.data) setApplications(response.data);
    else if (response.status === 401) setError(t('Сессия истекла. Выйдите и войдите снова.'));
    else if (response.status === 403) setError(t('У этой учётной записи нет доступа к заявкам участника.'));
    else if (response.status >= 400) setError(response.error?.message || t('Ошибка при загрузке заявок'));
  }, [navigate, t]);


  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/', { replace: true });
        return;
      }
      try {
        const [accountResponse, contestsResponse, applicationsResponse] = await Promise.all([
          usersApi.getAccountData(token),
          applicationsApi.getContests(token),
          applicationsApi.getUserApplications(token),
        ]);
        if (!active) return;
        if (accountResponse.data) {
          setAccount(accountResponse.data);
          if (!isEditing) {
            setProfileDraft({
              firstName: accountResponse.data.firstName ?? '',
              lastName: accountResponse.data.lastName ?? '',
            });
          }
        } else {
          setError(accountResponse.error?.message || t('Не удалось загрузить профиль'));
        }
        if (contestsResponse.data) setContests(contestsResponse.data);
        if (applicationsResponse.data) setApplications(applicationsResponse.data);
        else if (applicationsResponse.error) setError(applicationsResponse.error.message);
      } catch (loadError) {
        if (active) setError(loadError instanceof Error ? loadError.message : t('Ошибка при загрузке заявок'));
      } finally {
        if (active) setLoading(false);
      }
    };
    void load();
    return () => { active = false; };
  }, [navigate, t, isEditing]);

  const goToApplication = () => {
    if (!contests.length) {
      setError(t('Список конкурсов пока не загружен. Обновите страницу через несколько секунд.'));
      return;
    }
    navigate(`/application/${contests[0].conId}`);
  };

  const handleSaveProfile = async (event: React.FormEvent) => {
    event.preventDefault();
    const token = localStorage.getItem('token');
    if (!token || savingProfile) return;

    const firstName = profileDraft.firstName.trim();
    const lastName = profileDraft.lastName.trim();
    if (!firstName || !lastName) {
      setError(t('Имя и фамилия не могут быть пустыми'));
      return;
    }

    setSavingProfile(true);
    setError('');
    try {
      const response = await usersApi.updateAccountData({ firstName, lastName }, token);
      if (response.status !== 204 && response.status !== 200) {
        throw new Error(response.error?.message || t('Не удалось сохранить профиль'));
      }
      setAccount(previous => previous ? { ...previous, firstName, lastName } : previous);
      setIsEditing(false);
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : t('Не удалось сохранить профиль'));
    } finally {
      setSavingProfile(false);
    }
  };

  const handleEditProfile = () => {
    if (!account) return;
    setProfileDraft({ firstName: account.firstName ?? '', lastName: account.lastName ?? '' });
    setError('');
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setProfileDraft({
      firstName: account?.firstName ?? '',
      lastName: account?.lastName ?? '',
    });
    setIsEditing(false);
    setError('');
  };

  const handleLogout = () => {
    signOut();
    navigate('/', { replace: true });
  };

  const handleDeleteApplication = async (id: number) => {
    if (!window.confirm(t('Вы уверены, что хотите удалить заявку?'))) return;
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return;
    }
    try {
      const response = await applicationsApi.deleteApplication(id, token);
      if (response.status === 204 || response.status === 200) await fetchApplications();
      else setError(response.error?.message || t('Ошибка при удалении заявки'));
    } catch {
      setError(t('Ошибка при удалении заявки'));
    }
  };

  const stats = {
    total: applications.length,
    pending: applications.filter(application => application.status === 'PENDING').length,
    accepted: applications.filter(application => application.status === 'ACCEPTED').length,
    rejected: applications.filter(application => application.status === 'REJECTED').length,
  };

  const statusText = (status?: string) => {
    switch (status) {
      case 'PENDING': return t('На рассмотрении');
      case 'ACCEPTED': return t('Принята');
      case 'REJECTED': return t('Отклонена');
      default: return status || t('Нет данных');
    }
  };

  const statusClass = (status?: string) => {
    switch (status) {
      case 'PENDING': return css.statusPending;
      case 'ACCEPTED': return css.statusAccepted;
      case 'REJECTED': return css.statusRejected;
      default: return '';
    }
  };

  const getWorkLink = (link: string) => `${API_BASE_URL}${link}`;
  const visibleApplications = filter
    ? applications.filter(application => application.status === filter)
    : applications;

  return (
    <>
      <DecorLines />
      <main className={css.container}>
        <section className={css.header}>
          <h1 className={css.title}>{t('Личный кабинет')}</h1>
          <div className={css.headerActions}>
            {role === 'PARTICIPANT' && <Button variant="secondary" onClick={goToApplication}>+ {t('Подать заявку')}</Button>}
            <Button variant="secondary" onClick={handleLogout}>{t('Выйти')}</Button>
          </div>
        </section>

        <section className={css.profileSummary}>
          {isEditing ? (
            <form className={css.profileEditForm} onSubmit={handleSaveProfile}>
              <div className={css.profileFields}>
                <label>
                  <span>{t('Имя')}</span>
                  <input value={profileDraft.firstName} maxLength={50} onChange={event => setProfileDraft(previous => ({ ...previous, firstName: event.target.value }))} autoComplete="given-name" required />
                </label>
                <label>
                  <span>{t('Фамилия')}</span>
                  <input value={profileDraft.lastName} maxLength={50} onChange={event => setProfileDraft(previous => ({ ...previous, lastName: event.target.value }))} autoComplete="family-name" required />
                </label>
              </div>
              <div className={css.profileEditActions}>
                <Button type="submit" variant="primary" disabled={savingProfile}>{savingProfile ? t('Сохранение...') : t('Сохранить')}</Button>
                <Button type="button" variant="secondary" onClick={handleCancelEdit} disabled={savingProfile}>{t('Отмена')}</Button>
              </div>
            </form>
          ) : (
            <>
              <div className={css.profileIdentity}>
                <strong>{t('Пользователь')}: {account ? `${account.firstName} ${account.lastName}`.trim() : '—'}</strong>
                <span>{t('Email')}: {account?.email ?? '—'}</span>
              </div>
              <Button variant="secondary" onClick={handleEditProfile} disabled={!account}>{t('Редактировать профиль')}</Button>
            </>
          )}
        </section>

        {error && <div className={css.errorMessage} role="alert">{error}</div>}

        <section className={css.stats}>
          <article className={css.statCard}><div className={css.statValue}>{loading ? '…' : stats.total}</div><div className={css.statLabel}>{t('Всего заявок')}</div></article>
          <article className={css.statCard}><div className={css.statValue}>{loading ? '…' : stats.pending}</div><div className={css.statLabel}>{t('На рассмотрении')}</div></article>
          <article className={css.statCard}><div className={css.statValue}>{loading ? '…' : stats.accepted}</div><div className={css.statLabel}>{t('Принято')}</div></article>
          <article className={css.statCard}><div className={css.statValue}>{loading ? '…' : stats.rejected}</div><div className={css.statLabel}>{t('Отклонено')}</div></article>
        </section>

        <div className={css.filters}>
          <select className={css.filterSelect} value={filter} onChange={event => setFilter(event.target.value)}>
            <option value="">{t('Все заявки')}</option>
            <option value="PENDING">{t('На рассмотрении')}</option>
            <option value="ACCEPTED">{t('Принятые')}</option>
            <option value="REJECTED">{t('Отклоненные')}</option>
          </select>
        </div>

        <h2 className={css.sectionTitle}>{t('Мои заявки')}</h2>
        {loading ? <div className={css.loadingState}>{t('Загрузка...')}</div> : visibleApplications.length === 0 ? (
          <div className={css.emptyState}>
            <p className={css.emptyText}>{applications.length === 0 ? t('У вас пока нет заявок') : t('По выбранному фильтру заявок нет')}</p>
            {applications.length === 0 && role === 'PARTICIPANT' && <Button variant="primary" onClick={goToApplication}>{t('Подать первую заявку')}</Button>}
          </div>
        ) : (
          <div className={css.applicationsList}>
            {visibleApplications.map(application => (
              <article key={application.appId} className={css.applicationCard}>
                <div className={css.applicationHeader}>
                  <span className={css.applicationContest}>{application.contestName}</span>
                  {application.status && <span className={`${css.applicationStatus} ${statusClass(application.status)}`}>{statusText(application.status)}</span>}
                </div>
                <div className={css.applicationDetails}>
                  <div><strong>{t('Работа:')}</strong> {application.name}</div>
                  <div><strong>{t('Описание:')}</strong> {application.description}</div>
                  <div><strong>{t('Файлов:')}</strong> {application.worksCount}</div>
                  {!!application.linksToWorks?.length && (
                    <div className={css.applicationFiles}>
                      <strong>{t('Файлы:')}</strong>{' '}
                      {application.linksToWorks.map((link, index) => (
                        <a key={`${link}-${index}`} href={getWorkLink(link)} target="_blank" rel="noopener noreferrer">
                          {t('Скачать')} {application.linksToWorks!.length > 1 ? `№${index + 1}` : ''}
                        </a>
                      ))}
                    </div>
                  )}
                  {application.rejectionReason && <div className={css.rejectionReason}><strong>{t('Причина отказа:')}</strong> {application.rejectionReason}</div>}
                </div>
                {(application.status === 'PENDING' || !application.status) && (
                  <div className={css.applicationActions}>
                    <button className={css.deleteBtn} onClick={() => void handleDeleteApplication(application.appId)}>{t('Удалить')}</button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </main>
    </>
  );
};
