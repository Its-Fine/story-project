import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { Button } from '../../components/UI/Button';
import { applicationsApi } from '../../api/applications';
import { authApi } from '../../api/auth';
import { API_BASE_URL } from '../../api/client';
import { Application, Contest } from '../../api/applications';
import css from './index.module.scss';

export const ProfilePage = () => {
  const navigate = useNavigate();
  const [applications, setApplications] = useState<Application[]>([]);
  const [contests, setContests] = useState<Contest[]>([]);
  const [role, setRole] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('');
  const [error, setError] = useState<string>('');

  const fetchApplications = React.useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');

      if (!token) {
        navigate('/');
        return;
      }

      const response = await applicationsApi.getUserApplications(
        token
      );

      if (response.data) {
        setApplications(response.data);
      } else if (response.status === 401) {
        setError('Сессия истекла. Выйдите и войдите снова.');
      } else if (response.status === 403) {
        setError('У этой учётной записи нет доступа к заявкам участника.');
      } else if (response.status >= 400) {
        setError(response.error?.message || 'Ошибка при загрузке заявок');
      }
    } catch (err) {
      setError('Ошибка при загрузке заявок');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return;
    }

    Promise.all([authApi.me(token), applicationsApi.getContests(token)])
      .then(([userResponse, contestsResponse]) => {
        if (userResponse.data?.role) {
          setRole(userResponse.data.role);
        }
        if (contestsResponse.data) {
          setContests(contestsResponse.data);
        }
      })
      .catch((err) => {
        console.error('Ошибка загрузки роли или конкурсов', err);
      });
  }, [navigate]);

  useEffect(() => {
    void fetchApplications();
  }, [fetchApplications]);

  const goToApplication = () => {
    if (contests.length === 0) {
      setError('Список конкурсов пока не загружен. Обновите страницу через несколько секунд.');
      return;
    }
    navigate(`/application/${contests[0].conId}`);
  };

  const handleLogout = async () => {
    const token = localStorage.getItem('token');
    if (token) {
      await authApi.logout(token);
    }
    localStorage.removeItem('token');
    navigate('/');
  };

  const handleDeleteApplication = async (id: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить заявку?')) return;

    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return;
    }

    try {
      const response = await applicationsApi.deleteApplication(id, token);

      if (response.status === 204) {
        fetchApplications();
      }
    } catch (err) {
      setError('Ошибка при удалении заявки');
    }
  };

  const stats = {
    total: applications.length,
    pending: applications.filter(a => a.status === 'PENDING').length,
    accepted: applications.filter(a => a.status === 'ACCEPTED').length,
    rejected: applications.filter(a => a.status === 'REJECTED').length
  };

  const getStatusText = (status?: string) => {
    switch (status) {
      case 'PENDING': return 'На рассмотрении';
      case 'ACCEPTED': return 'Принята';
      case 'REJECTED': return 'Отклонена';
      default: return status || 'Нет данных';
    }
  };

  const getStatusClass = (status?: string) => {
    switch (status) {
      case 'PENDING': return css.statusPending;
      case 'ACCEPTED': return css.statusAccepted;
      case 'REJECTED': return css.statusRejected;
      default: return '';
    }
  };

  const getWorkLink = (link: string) => `${API_BASE_URL}${link}`;

  const visibleApplications = filter
    ? applications.filter(app => app.status === filter)
    : applications;

  return (
    <>
      <DecorLines />
      <div className={css.container}>
        <div className={css.header}>
          <h1 className={css.title}>Личный кабинет</h1>
          {role === 'PARTICIPANT' && (
            <div className={css.headerButtons}>
              <Button variant="primary" onClick={goToApplication}>
                + Подать заявку
              </Button>
            </div>
          )}
        </div>

        <div className={css.stats}>
          <div className={css.statCard}>
            <div className={css.statValue}>{stats.total}</div>
            <div className={css.statLabel}>Всего заявок</div>
          </div>
          <div className={css.statCard}>
            <div className={css.statValue}>{stats.pending}</div>
            <div className={css.statLabel}>На рассмотрении</div>
          </div>
          <div className={css.statCard}>
            <div className={css.statValue}>{stats.accepted}</div>
            <div className={css.statLabel}>Принято</div>
          </div>
          <div className={css.statCard}>
            <div className={css.statValue}>{stats.rejected}</div>
            <div className={css.statLabel}>Отклонено</div>
          </div>
        </div>

        <div className={css.filters}>
          <select
            className={css.filterSelect}
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            <option value="">Все заявки</option>
            <option value="PENDING">На рассмотрении</option>
            <option value="ACCEPTED">Принятые</option>
            <option value="REJECTED">Отклоненные</option>
          </select>
        </div>

        <h2 className={css.sectionTitle}>Мои заявки</h2>

        {error && <div className={css.errorMessage}>{error}</div>}

        {loading ? (
          <div className={css.loadingState}>Загрузка...</div>
        ) : visibleApplications.length === 0 ? (
          <div className={css.emptyState}>
            <p className={css.emptyText}>
              {applications.length === 0
                ? 'У вас пока нет заявок'
                : 'По выбранному фильтру заявок нет'}
            </p>
            {applications.length === 0 && role === 'PARTICIPANT' && (
              <Button variant="primary" onClick={goToApplication}>
                Подать первую заявку
              </Button>
            )}
          </div>
        ) : (
          <div className={css.applicationsList}>
            {visibleApplications.map(app => (
              <div key={app.appId} className={css.applicationCard}>
                <div className={css.applicationHeader}>
                  <span className={css.applicationContest}>{app.contestName}</span>
                  <span className={`${css.applicationStatus} ${getStatusClass(app.status)}`}>
                    {getStatusText(app.status)}
                  </span>
                </div>
                <div className={css.applicationDetails}>
                  <div><strong>Работа:</strong> {app.name}</div>
                  <div><strong>Описание:</strong> {app.description}</div>
                  <div><strong>Файлов:</strong> {app.worksCount}</div>
                  {app.linksToWorks && app.linksToWorks.length > 0 && (
                    <div className={css.applicationFiles}>
                      <strong>Файлы:</strong>{' '}
                      {app.linksToWorks.map((link, index) => (
                        <a
                          key={link}
                          href={getWorkLink(link)}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          Скачать {app.linksToWorks!.length > 1 ? `№${index + 1}` : ''}
                        </a>
                      ))}
                    </div>
                  )}
                  {app.rejectionReason && (
                    <div className={css.rejectionReason}>
                      <strong>Причина отказа:</strong> {app.rejectionReason}
                    </div>
                  )}
                </div>
                {(app.status === 'PENDING' || !app.status) && (
                  <div className={css.applicationActions}>
                    <button
                      className={css.deleteBtn}
                      onClick={() => handleDeleteApplication(app.appId)}
                    >
                      Удалить
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        <div className={css.logoutSection}>
          <Button variant="danger" onClick={handleLogout}>
            Выйти
          </Button>
        </div>
      </div>
    </>
  );
};