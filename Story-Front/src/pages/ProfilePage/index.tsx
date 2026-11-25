import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { Button } from '../../components/UI/Button';
import { applicationsApi } from '../../api/applications';
import { usersApi, AccountData } from '../../api/users';
import { API_BASE_URL } from '../../api/client';
import { Application, ApplicationMemberUpdate, Contest } from '../../api/applications';
import { useAuth } from '../../contexts/AuthContext';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './index.module.scss';

export const ProfilePage = () => {
  const navigate = useNavigate();
  const { permissions, signOut } = useAuth();
  const canReadApplications = permissions.includes('application:read:own');
  const canWriteApplications = permissions.includes('application:write:own');
  const { t } = useLanguage();
  const [account, setAccount] = useState<AccountData | null>(null);
  const [applications, setApplications] = useState<Application[]>([]);
  const [contests, setContests] = useState<Contest[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [passwordDraft, setPasswordDraft] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [savingPassword, setSavingPassword] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState('');
  const [profileDraft, setProfileDraft] = useState({ firstName: '', lastName: '' });
  const [editingApplicationId, setEditingApplicationId] = useState<number | null>(null);
  const [applicationDraft, setApplicationDraft] = useState({
    name: '', description: '', city: '', organization: '', mentorName: '',
  });
  const [memberDrafts, setMemberDrafts] = useState<ApplicationMemberUpdate[]>([]);
  const [savingApplication, setSavingApplication] = useState(false);

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
        const accountPromise = usersApi.getAccountData(token);
        const participantDataPromise = canReadApplications
          ? Promise.all([
              applicationsApi.getContests(token),
              applicationsApi.getUserApplications(token),
            ])
          : Promise.resolve(null);

        const [accountResponse, participantData] = await Promise.all([
          accountPromise,
          participantDataPromise,
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

        if (participantData) {
          const [contestsResponse, applicationsResponse] = participantData;
          if (contestsResponse.data) setContests(contestsResponse.data);
          else if (contestsResponse.error) setError(contestsResponse.error.message);

          if (applicationsResponse.data) setApplications(applicationsResponse.data);
          else if (applicationsResponse.error) setError(applicationsResponse.error.message);
        } else {
          setContests([]);
          setApplications([]);
        }
      } catch (loadError) {
        if (active) setError(loadError instanceof Error ? loadError.message : t('Ошибка при загрузке профиля'));
      } finally {
        if (active) setLoading(false);
      }
    };
    void load();
    return () => { active = false; };
  }, [navigate, t, isEditing, canReadApplications]);

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

  const handleChangePassword = async (event: React.FormEvent) => {
    event.preventDefault();
    const token = localStorage.getItem('token');
    if (!token || savingPassword) return;

    if (!/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\S+$).{8,255}$/.test(passwordDraft.newPassword)) {
      setError(t('Пароль должен содержать 8–255 символов, цифру, строчную и заглавную буквы без пробелов'));
      return;
    }
    if (passwordDraft.newPassword !== passwordDraft.confirmPassword) {
      setError(t('Новые пароли не совпадают'));
      return;
    }
    if (passwordDraft.currentPassword === passwordDraft.newPassword) {
      setError(t('Новый пароль должен отличаться от текущего'));
      return;
    }

    setSavingPassword(true);
    setError('');
    setPasswordMessage('');
    try {
      const response = await usersApi.updatePassword({
        currentPassword: passwordDraft.currentPassword,
        newPassword: passwordDraft.newPassword,
      }, token);
      if (response.status !== 204 && response.status !== 200) {
        throw new Error(response.error?.message === 'Wrong password'
          ? t('Текущий пароль указан неверно')
          : response.error?.message || t('Не удалось изменить пароль'));
      }
      setPasswordDraft({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setPasswordMessage(t('Пароль успешно изменён'));
      setIsChangingPassword(false);
    } catch (passwordError) {
      setError(passwordError instanceof Error ? passwordError.message : t('Не удалось изменить пароль'));
    } finally {
      setSavingPassword(false);
    }
  };

  const handleEditProfile = () => {
    if (!account) return;
    setProfileDraft({ firstName: account.firstName ?? '', lastName: account.lastName ?? '' });
    setError('');
    setIsChangingPassword(false);
    setIsEditing(true);
  };

  const handleStartPasswordChange = () => {
    setIsEditing(false);
    setError('');
    setPasswordMessage('');
    setIsChangingPassword(true);
  };

  const handleCancelPasswordChange = () => {
    setPasswordDraft({ currentPassword: '', newPassword: '', confirmPassword: '' });
    setIsChangingPassword(false);
    setError('');
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

  const handleEditApplication = (application: Application) => {
    setError('');
    setApplicationDraft({
      name: application.name ?? '',
      description: application.description ?? '',
      city: application.city ?? '',
      organization: application.organization ?? '',
      mentorName: application.mentorName ?? '',
    });
    setMemberDrafts((application.members ?? []).map(member => ({
      firstName: member.firstName, lastName: member.lastName, birthDate: member.birthDate,
    })));
    setEditingApplicationId(application.appId);
  };

  const handleCancelApplicationEdit = () => {
    setEditingApplicationId(null);
    setApplicationDraft({ name: '', description: '', city: '', organization: '', mentorName: '' });
    setMemberDrafts([]);
    setError('');
  };

  const handleSaveApplication = async (event: React.FormEvent, application: Application) => {
    event.preventDefault();
    const token = localStorage.getItem('token');
    if (!token || savingApplication) return;

    const name = applicationDraft.name.trim();
    const description = applicationDraft.description.trim();
    const city = applicationDraft.city.trim();
    if (!name || !description || !city) {
      setError(t('Заполните название, описание и населённый пункт'));
      return;
    }

    const members = application.isTeam ? memberDrafts.map(member => ({
      firstName: member.firstName.trim(), lastName: member.lastName.trim(), birthDate: member.birthDate,
    })) : [];

    if (application.isTeam && members.length === 0) {
      setError(t('Добавьте хотя бы одного участника команды'));
      return;
    }
    if (application.isTeam && (members.some(member =>
      !member.firstName || !member.lastName || !member.birthDate)
      || new Set(members.map(member => `${member.firstName.toLowerCase()}|${member.lastName.toLowerCase()}|${member.birthDate}`)).size !== members.length)) {
      setError(t('Проверьте данные участников: пустые поля и повторы недопустимы'));
      return;
    }

    setSavingApplication(true);
    setError('');
    try {
      const response = await applicationsApi.updateApplication(application.appId, {
        name,
        description,
        city,
        organization: applicationDraft.organization.trim(),
        mentorName: applicationDraft.mentorName.trim(),
        ...(application.isTeam ? { members } : {}),
      }, token);

      if (response.status !== 204 && response.status !== 200) {
        throw new Error(response.status === 410
          ? t('Приём заявок закрыт. Редактирование заявок временно недоступно.')
          : response.error?.message || t('Не удалось обновить заявку'));
      }

      setEditingApplicationId(null);
      await fetchApplications();
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : t('Не удалось обновить заявку'));
    } finally {
      setSavingApplication(false);
    }
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
      else setError(response.status === 410
        ? t('Приём заявок закрыт. Удаление заявок временно недоступно.')
        : response.error?.message || t('Ошибка при удалении заявки'));
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

  const downloadWork = async (workId: number, name: string) => {
    const token = localStorage.getItem('token');
    if (!token) return;
    const response = await fetch(`${API_BASE_URL}/works/${workId}/file`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) {
      setError(t('Не удалось скачать файл'));
      return;
    }
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
  };
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
            {canWriteApplications && <Button variant="secondary" onClick={goToApplication}>+ {t('Подать заявку')}</Button>}
            <Button variant="secondary" onClick={handleLogout}>{t('Выйти')}</Button>
          </div>
        </section>

        <section className={css.profileSummary}>
          {isChangingPassword ? (
            <div className={css.passwordEditor}>
              <h2>{t('Изменение пароля')}</h2>
              <form onSubmit={handleChangePassword}>
                <div className={css.profileFields}>
                  <label>
                    <span>{t('Текущий пароль')}</span>
                    <input type="password" autoComplete="current-password" value={passwordDraft.currentPassword}
                      onChange={event => setPasswordDraft(previous => ({ ...previous, currentPassword: event.target.value }))} required />
                  </label>
                  <label>
                    <span>{t('Новый пароль')}</span>
                    <input type="password" autoComplete="new-password" minLength={8} maxLength={255}
                      value={passwordDraft.newPassword}
                      onChange={event => setPasswordDraft(previous => ({ ...previous, newPassword: event.target.value }))} required />
                  </label>
                  <label>
                    <span>{t('Повторите новый пароль')}</span>
                    <input type="password" autoComplete="new-password" minLength={8} maxLength={255}
                      value={passwordDraft.confirmPassword}
                      onChange={event => setPasswordDraft(previous => ({ ...previous, confirmPassword: event.target.value }))} required />
                  </label>
                </div>
                <div className={css.profileEditActions}>
                  <Button type="submit" variant="primary" disabled={savingPassword}>
                    {savingPassword ? t('Сохранение...') : t('Изменить пароль')}
                  </Button>
                  <Button type="button" variant="secondary" onClick={handleCancelPasswordChange} disabled={savingPassword}>{t('Отмена')}</Button>
                </div>
              </form>
            </div>
          ) : isEditing ? (
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
              <div className={css.profileEditActions}>
                <Button variant="secondary" onClick={handleEditProfile} disabled={!account}>{t('Редактировать профиль')}</Button>
                <Button variant="secondary" onClick={handleStartPasswordChange}>{t('Изменить пароль')}</Button>
              </div>
            </>
          )}
        </section>

        {passwordMessage && <div className={css.successMessage} role="status">{passwordMessage}</div>}
        {error && <div className={css.errorMessage} role="alert">{error}</div>}

        {canReadApplications && (
          <>
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
                {applications.length === 0 && canWriteApplications && <Button variant="primary" onClick={goToApplication}>{t('Подать первую заявку')}</Button>}
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
                  {!!application.works?.length && (
                    <div className={css.applicationFiles}>
                      <strong>{t('Файлы:')}</strong>{' '}
                      {application.works.map(work => (
                        <button type="button" key={work.workId} onClick={() => void downloadWork(work.workId, work.name)}>
                          {t('Скачать')} {work.name}
                        </button>
                      ))}
                    </div>
                  )}
                  {!!application.works?.length && (
                    <div className={css.applicationFiles}>
                      <strong>{t('Оценки работ:')}</strong>{' '}
                      {application.works.map(work => (
                        <span key={work.workId}>
                          {work.name}: {work.isRated ? `${work.rate}/100` : t('Без оценки')}
                        </span>
                      ))}
                    </div>
                  )}
                  {application.rejectionReason && <div className={css.rejectionReason}><strong>{t('Причина отказа:')}</strong> {application.rejectionReason}</div>}
                </div>
                {editingApplicationId === application.appId && (
                  <form className={css.applicationEditForm} onSubmit={event => void handleSaveApplication(event, application)}>
                    <label>
                      <span>{t('Название работы')}</span>
                      <input value={applicationDraft.name} maxLength={200} onChange={event => setApplicationDraft(previous => ({ ...previous, name: event.target.value }))} required />
                    </label>
                    <label>
                      <span>{t('Описание работы')}</span>
                      <textarea value={applicationDraft.description} rows={3} onChange={event => setApplicationDraft(previous => ({ ...previous, description: event.target.value }))} required />
                    </label>
                    <div className={css.applicationEditRow}>
                      <label>
                        <span>{t('Населенный пункт')}</span>
                        <input value={applicationDraft.city} maxLength={100} onChange={event => setApplicationDraft(previous => ({ ...previous, city: event.target.value }))} required />
                      </label>
                      <label>
                        <span>{t('Организация')}</span>
                        <input value={applicationDraft.organization} maxLength={200} onChange={event => setApplicationDraft(previous => ({ ...previous, organization: event.target.value }))} />
                      </label>
                      <label>
                        <span>{t('ФИО наставника')}</span>
                        <input value={applicationDraft.mentorName} maxLength={100} onChange={event => setApplicationDraft(previous => ({ ...previous, mentorName: event.target.value }))} />
                      </label>
                    </div>
                    {application.isTeam && (
                      <div>
                        <strong>{t('Участники команды')}</strong>
                        {memberDrafts.map((member, index) => (
                          <div className={css.applicationEditRow} key={index}>
                            <input aria-label={t('Имя участника')} value={member.firstName}
                              onChange={event => setMemberDrafts(previous => previous.map((item, i) =>
                                i === index ? { ...item, firstName: event.target.value } : item))} />
                            <input aria-label={t('Фамилия участника')} value={member.lastName}
                              onChange={event => setMemberDrafts(previous => previous.map((item, i) =>
                                i === index ? { ...item, lastName: event.target.value } : item))} />
                            <input aria-label={t('Дата рождения участника')} type="date" value={member.birthDate}
                              onChange={event => setMemberDrafts(previous => previous.map((item, i) =>
                                i === index ? { ...item, birthDate: event.target.value } : item))} />
                            <button type="button" onClick={() => setMemberDrafts(previous => previous.filter((_, i) => i !== index))}>{t('Удалить участника')}</button>
                          </div>
                        ))}
                        <button type="button" onClick={() => setMemberDrafts(previous => [...previous,
                          { firstName: '', lastName: '', birthDate: '' }])}>{t('Добавить участника')}</button>
                      </div>
                    )}
                    <div className={css.applicationEditActions}>
                      <Button type="submit" variant="primary" disabled={savingApplication}>{savingApplication ? t('Сохранение...') : t('Сохранить')}</Button>
                      <Button type="button" variant="secondary" onClick={handleCancelApplicationEdit} disabled={savingApplication}>{t('Отмена')}</Button>
                    </div>
                  </form>
                )}
                {canWriteApplications && (application.status === 'PENDING' || !application.status) && (
                  <div className={css.applicationActions}>
                    <button
                      className={css.editBtn}
                      onClick={() => editingApplicationId === application.appId ? handleCancelApplicationEdit() : handleEditApplication(application)}
                      disabled={savingApplication}
                    >
                      {editingApplicationId === application.appId ? t('Отмена') : t('Редактировать')}
                    </button>
                    <button className={css.deleteBtn} onClick={() => void handleDeleteApplication(application.appId)}>{t('Удалить')}</button>
                  </div>
                )}
              </article>
            ))}
              </div>
            )}
          </>
        )}
      </main>
    </>
  );
};
