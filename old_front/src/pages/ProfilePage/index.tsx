import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { Button } from '../../components/UI/Button';
import { applicationsApi } from '../../api/applications';
import { authApi } from '../../api/auth';
import { Application } from '../../types';
import css from './index.module.scss';

export const ProfilePage = () => {
  const navigate = useNavigate();
  const [applications, setApplications] = useState<Application[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('');
  const [error, setError] = useState<string>('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return;
    }
    fetchApplications();
  }, [filter, navigate]);

  const fetchApplications = async () => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');

      if (!token) {
        navigate('/');
        return;
      }

      const response = await applicationsApi.getUserApplications(
        token,
        filter || undefined
      );

      if (response.data?.results) {
        setApplications(response.data.results);
      }
    } catch (err) {
      setError('Ошибка при загрузке заявок');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    const token = localStorage.getItem('token');
    if (token) {
      await authApi.logout(token);
    }
    localStorage.removeItem('token');
    navigate('/');
  };

  const handleDeleteApplication = async (id: number, contestId: number) => {
    if (!window.confirm('Вы уверены, что хотите удалить заявку?')) return;

    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return;
    }

    try {
      const response = await applicationsApi.deleteApplication(
        { application_id: id },  // ← обернули в объект
        contestId.toString(),
        token
      );

      if (response.status === 200) {
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

  const getStatusText = (status: string) => {
    switch(status) {
      case 'PENDING': return 'На рассмотрении';
      case 'ACCEPTED': return 'Принята';
      case 'REJECTED': return 'Отклонена';
      default: return status;
    }
  };

  const getStatusClass = (status: string) => {
    switch(status) {
      case 'PENDING': return css.statusPending;
      case 'ACCEPTED': return css.statusAccepted;
      case 'REJECTED': return css.statusRejected;
      default: return '';
    }
  };

  return (
    <>
      <DecorLines />
      <div className={css.container}>
        <div className={css.header}>
          <h1 className={css.title}>Личный кабинет</h1>
          <div className={css.headerButtons}>
            <Button variant="primary" onClick={() => navigate('/application/123')}>
              + Подать заявку
            </Button>
          </div>
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
        ) : applications.length === 0 ? (
          <div className={css.emptyState}>
            <p className={css.emptyText}>У вас пока нет заявок</p>
            <Button variant="primary" onClick={() => navigate('/application/123')}>
              Подать первую заявку
            </Button>
          </div>
        ) : (
          <div className={css.applicationsList}>
            {applications.map(app => (
              <div key={app.id} className={css.applicationCard}>
                <div className={css.applicationHeader}>
                  <span className={css.applicationContest}>{app.contest_title}</span>
                  <span className={`${css.applicationStatus} ${getStatusClass(app.status)}`}>
                    {getStatusText(app.status)}
                  </span>
                </div>
                <div className={css.applicationDetails}>
                  <div><strong>Номинация:</strong> {app.nomination_name}</div>
                  <div><strong>Работа:</strong> {app.name}</div>
                  <div><strong>Описание:</strong> {app.annotation}</div>
                  {app.link_to_work && (
                    <div>
                      <strong>Файл:</strong>{' '}
                      <a href={app.link_to_work} target="_blank" rel="noopener noreferrer">
                        Скачать
                      </a>
                    </div>
                  )}
                  {app.rejection_reason && (
                    <div className={css.rejectionReason}>
                      <strong>Причина отказа:</strong> {app.rejection_reason}
                    </div>
                  )}
                </div>
                {app.status === 'PENDING' && (
                  <div className={css.applicationActions}>
                    <button
                      className={css.deleteBtn}
                      onClick={() => handleDeleteApplication(app.id, app.contest_id)}
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