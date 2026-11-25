import React, { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { Notification } from '../../components/UI/Notification';
import { applicationsApi, Contest } from '../../api/applications';
import { usersApi } from '../../api/users';
import { storageApi } from '../../api/storage';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './index.module.scss';

const createEmptyFormData = () => ({
  firstName: '',
  lastName: '',
  email: '',
  birthDate: '',
  city: '',
  organization: '',
  nomination: '',
  workTitle: '',
  description: '',
  mentorName: ''
});

type ApplicationFormData = ReturnType<typeof createEmptyFormData>;
type ApplicationMemberDraft = { firstName: string; lastName: string; birthDate: string };

interface StoredApplicationDraft {
  formData: ApplicationFormData;
  isTeam: boolean;
  isCurator: boolean;
  members: ApplicationMemberDraft[];
}

const getDraftOwner = () => {
  const token = localStorage.getItem('token');
  if (!token) return 'anonymous';

  try {
    const payload = token.split('.')[1];
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    return JSON.parse(atob(padded)).sub || 'unknown';
  } catch {
    return 'unknown';
  }
};

export const ApplicationPage = () => {
  const { contestId } = useParams<{ contestId: string }>();
  const navigate = useNavigate();
  const { t, language } = useLanguage();
  const draftStorageKey = `story-application-draft:${getDraftOwner()}:${contestId || 'default'}`;

  const [formData, setFormData] = useState(createEmptyFormData);

  const [isTeam, setIsTeam] = useState(false);
  const [isCurator, setIsCurator] = useState(false);
  const [members, setMembers] = useState<ApplicationMemberDraft[]>([]);
  const [draftLoaded, setDraftLoaded] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [isUploading, setIsUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [notification, setNotification] = useState<{
    show: boolean;
    message: string;
    type: 'success' | 'error'
  }>({
    show: false,
    message: '',
    type: 'success'
  });

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return;
    }
    void Promise.all([applicationsApi.getContests(token), usersApi.getAccountData(token)]).then(([catalogue, account]) => {
      if (catalogue.data) setContests(catalogue.data);
      else showNotification(catalogue.error?.message || t('Не удалось загрузить номинации'), 'error');
      if (account.data) {
        setFormData(previous => ({
          ...previous,
          firstName: account.data!.firstName,
          lastName: account.data!.lastName,
          email: account.data!.email,
          birthDate: account.data!.birthDate,
        }));
      }
    });
  }, [navigate, t]);

  const [contests, setContests] = useState<Contest[]>([]);
  const [availableNominations, setAvailableNominations] = useState<Contest[]>([]);

  const calculateAge = useCallback((birthDate: string): number => {
    const birth = new Date(birthDate);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }, []);

  const validateEmail = (email: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  };

  const validateAge = (birthDate: string): { isValid: boolean; message: string } => {
    if (!birthDate) return { isValid: false, message: t('Введите дату рождения') };
    const age = calculateAge(birthDate);
    if (!contests.some(contest => age >= (contest.startAge ?? 0) && age <= (contest.endAge ?? 100))) {
      return { isValid: false, message: t('Для этого возраста нет доступных номинаций') };
    }
    return { isValid: true, message: '' };
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { id, value } = e.target;
    setFormData(prev => ({ ...prev, [id]: value }));
    setErrors(prev => ({ ...prev, [id]: '' }));
  };

  const handleEmailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setFormData(prev => ({ ...prev, email: value }));
    if (value && !validateEmail(value)) {
      setErrors(prev => ({ ...prev, email: t('Введите корректный email') }));
    } else {
      setErrors(prev => ({ ...prev, email: '' }));
    }
  };

  const updateNominationsByBirthDate = useCallback((birthDate: string) => {
    if (!birthDate) {
      setAvailableNominations([]);
      return;
    }
    const age = calculateAge(birthDate);
    setAvailableNominations(contests.filter(contest =>
      age >= (contest.startAge ?? 0) && age <= (contest.endAge ?? 100)));
  }, [calculateAge, contests]);

  useEffect(() => {
    setDraftLoaded(false);
    try {
      const savedDraft = localStorage.getItem(draftStorageKey);
      if (!savedDraft) return;

      const draft = JSON.parse(savedDraft) as StoredApplicationDraft;
      if (!draft.formData) return;

      setFormData(previous => ({ ...previous, ...draft.formData }));
      setIsTeam(Boolean(draft.isTeam));
      setIsCurator(Boolean(draft.isCurator));
      setMembers(Array.isArray(draft.members) ? draft.members : []);

      const hasContent = Object.values(draft.formData).some(Boolean) || Boolean(draft.members?.length);
      if (hasContent) {
        setNotification({
          show: true,
          message: t('Черновик восстановлен. Файлы нужно прикрепить заново.'),
          type: 'success'
        });
      }
    } catch {
      localStorage.removeItem(draftStorageKey);
    } finally {
      setDraftLoaded(true);
    }
  }, [draftStorageKey, t]);

  useEffect(() => {
    updateNominationsByBirthDate(isCurator ? members[0]?.birthDate || '' : formData.birthDate);
  }, [formData.birthDate, isCurator, members, updateNominationsByBirthDate]);

  useEffect(() => {
    if (!contests.length || !draftLoaded) return;
    setFormData(previous => {
      const selected = availableNominations.find(nomination =>
        String(nomination.conId) === previous.nomination);
      if (selected) return previous;
      const initial = availableNominations.find(nomination => String(nomination.conId) === contestId);
      return { ...previous, nomination: initial ? String(initial.conId) : '' };
    });
  }, [availableNominations, contestId, contests.length, draftLoaded]);

  useEffect(() => {
    if (!draftLoaded) return;

    const hasContent = Object.values(formData).some(Boolean) || members.length > 0 || isTeam || isCurator;
    if (!hasContent) {
      localStorage.removeItem(draftStorageKey);
      return;
    }

    const draft: StoredApplicationDraft = {
      formData: { ...formData, firstName: '', lastName: '', email: '', birthDate: '' },
      isTeam, isCurator, members,
    };
    localStorage.setItem(draftStorageKey, JSON.stringify(draft));
  }, [draftLoaded, draftStorageKey, formData, isCurator, isTeam, members]);

  const handleBirthDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const date = e.target.value;
    setFormData(prev => ({ ...prev, birthDate: date }));
    const ageValidation = validateAge(date);
    if (date && !isCurator && !ageValidation.isValid) {
      setErrors(prev => ({ ...prev, birthDate: ageValidation.message }));
    } else {
      setErrors(prev => ({ ...prev, birthDate: '' }));
    }
    updateNominationsByBirthDate(isCurator ? (members[0]?.birthDate || '') : date);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files) {
      if (uploadedFiles.length + files.length > 10) {
        showNotification(t('Можно загрузить не более 10 файлов'), 'error');
        return;
      }
      const newFiles = Array.from(files).filter(file => file.size <= 20 * 1024 * 1024);
      if (newFiles.length !== files.length) {
        showNotification(t('Размер файла не должен превышать 20 МБ'), 'error');
        return;
      }
      if (new Set([...uploadedFiles, ...newFiles].map(file => file.name)).size !== uploadedFiles.length + newFiles.length) {
        showNotification(t('Файлы с одинаковыми именами нельзя прикрепить к одной заявке'), 'error');
        return;
      }
      setUploadedFiles(prev => [...prev, ...newFiles]);
      setErrors(prev => ({ ...prev, files: '' }));
    }
  };

  const removeFile = (index: number) => {
    setUploadedFiles(prev => prev.filter((_, i) => i !== index));
    if (uploadedFiles.length <= 1) {
      setErrors(prev => ({ ...prev, files: t('Загрузите файл работы') }));
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + (language === 'en' ? ' B' : ' Б');
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + (language === 'en' ? ' KB' : ' КБ');
    return (bytes / (1024 * 1024)).toFixed(1) + (language === 'en' ? ' MB' : ' МБ');
  };

  const updateMember = (index: number, field: 'firstName' | 'lastName' | 'birthDate', value: string) => {
    setMembers(previous => previous.map((member, i) => i === index ? { ...member, [field]: value } : member));
    if (isCurator && index === 0 && field === 'birthDate') updateNominationsByBirthDate(value);
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};
    let isValid = true;

    const required = ['city', 'nomination', 'workTitle', 'description'];

    required.forEach(field => {
      if (!formData[field as keyof typeof formData]) {
        newErrors[field] = t('Заполните поле');
        isValid = false;
      }
    });

    if (!availableNominations.some(nomination => String(nomination.conId) === formData.nomination)) {
      newErrors.nomination = t('Выберите доступную номинацию');
      isValid = false;
    }

    if (formData.birthDate && !isCurator) {
      const ageValidation = validateAge(formData.birthDate);
      if (!ageValidation.isValid) {
        newErrors.birthDate = ageValidation.message;
        isValid = false;
      }
    }

    if (isTeam) {
      if (members.length === 0) {
        newErrors.members = t('Добавьте хотя бы одного участника команды');
        isValid = false;
      }
      members.forEach((member, index) => {
        if (!member.firstName.trim() || !member.lastName.trim() || !member.birthDate) {
          newErrors[`member-${index}`] = t('Заполните ФИО и дату рождения участника');
          isValid = false;
        } else {
          const selected = contests.find(contest => String(contest.conId) === formData.nomination);
          const age = calculateAge(member.birthDate);
          if (selected && (age < (selected.startAge ?? 0) || age > (selected.endAge ?? 100))) {
            newErrors[`member-${index}`] = t('Возраст участника не подходит для номинации');
            isValid = false;
          }
        }
      });
    }

    if (uploadedFiles.length === 0) {
      newErrors.files = t('Загрузите файл работы');
      isValid = false;
    } else if (uploadedFiles.length > 10) {
      newErrors.files = t('Можно загрузить не более 10 файлов');
      isValid = false;
    }
    if (uploadedFiles.reduce((total, file) => total + file.size, 0) > 20 * 1024 * 1024) {
      newErrors.files = t('Общий размер файлов не должен превышать 20 МБ');
      isValid = false;
    }
    const memberKeys = members.map(member =>
      `${member.firstName.trim().toLowerCase()}|${member.lastName.trim().toLowerCase()}|${member.birthDate}`);
    if (isTeam && new Set(memberKeys).size !== memberKeys.length) {
      newErrors.members = t('В составе команды есть повторяющиеся участники');
      isValid = false;
    }

    setErrors(newErrors);
    return isValid;
  };

  const uploadFiles = async (appId: number): Promise<boolean> => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return false;
    }

    setIsUploading(true);
    try {
      for (let i = 0; i < uploadedFiles.length; i++) {
        const file = uploadedFiles[i];
        setIsUploading(true);
        await storageApi.uploadWorkWithProgress(
          file,
          appId,
          token,
          (progress) => setUploadProgress(progress)
        );
      }
      return true;
    } catch (error) {
      showNotification(error instanceof Error ? error.message : t('Не удалось загрузить файл'), 'error');
      return false;
    } finally {
      setIsUploading(false);
      setUploadProgress(0);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;
    if (submitting) return;

    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return;
    }

    setSubmitting(true);
    const nominationName = availableNominations.find(
      nomination => nomination.conId === Number(formData.nomination)
    )?.name;
    if (!nominationName) {
      showNotification(t('Выберите доступную номинацию'), 'error');
      setSubmitting(false);
      return;
    }
    const response = await applicationsApi.createApplication(
      {
        name: formData.workTitle,
        description: formData.description,
        city: formData.city,
        organization: formData.organization || undefined,
        mentorName: formData.mentorName || undefined,
        members: isTeam ? members.map(member => ({
          firstName: member.firstName.trim(),
          lastName: member.lastName.trim(),
          birthDate: member.birthDate,
        })) : [],
        isTeam,
        isCurator: isTeam && isCurator,
      },
      formData.nomination,
      token
    );

    const appId = response.data?.appId;
    if (response.status !== 201 || !appId) {
      const message = response.status === 410
        ? t('Приём заявок закрыт. Новые заявки временно не принимаются.')
        : response.error?.message || t('Не удалось создать заявку');
      showNotification(message, 'error');
      setSubmitting(false);
      return;
    }

    showNotification(t('Заявка создана. Загружаем файлы...'), 'success');
    const filesUploaded = await uploadFiles(appId);
    if (!filesUploaded) {
      const cleanup = await applicationsApi.deleteApplication(appId, token);
      showNotification(cleanup.status === 204
        ? t('Загрузка не завершена. Заявка удалена, попробуйте снова.')
        : t('Загрузка не завершена. Заявка сохранена в личном кабинете — проверьте её перед повторной подачей.'), 'error');
      setSubmitting(false);
      return;
    }

    showNotification(t('Заявка отправлена!'), 'success');
    localStorage.removeItem(draftStorageKey);
    setSubmitting(false);
    setTimeout(() => navigate('/profile'), 1500);
  };

  const handleClear = () => {
    localStorage.removeItem(draftStorageKey);
    setFormData(previous => ({
      ...createEmptyFormData(),
      firstName: previous.firstName,
      lastName: previous.lastName,
      email: previous.email,
      birthDate: previous.birthDate,
    }));
    setUploadedFiles([]);
    setErrors({});
    setIsTeam(false);
    setIsCurator(false);
    setMembers([]);
  };

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ show: true, message, type });
  };

  return (
    <>
      <DecorLines />
      <div className={css.container}>
        <div className={css.hero}>
          <h1 className={css.heroTitle}>{t('Подача заявки')}</h1>
        </div>

        <div className={css.formCard}>
          <form onSubmit={handleSubmit}>
            <div className={css.formSection}>
              <h2 className={css.sectionTitle}>{t('Личные данные')}</h2>

              <div className={css.formRow}>
                <div className={css.formGroup}>
                  <label className={css.label}>
                    {t('Имя')} <span className={css.required}>*</span>
                  </label>
                  <input
                    type="text"
                    id="firstName"
                    value={formData.firstName}
                    onChange={handleChange}
                    onBlur={() => validateForm()}
                    className={`${css.formControl} ${errors.firstName ? css.error : ''}`}
                    maxLength={50}
                    readOnly
                  />
                  {errors.firstName && <div className={css.errorMessage}>{errors.firstName}</div>}
                </div>

                <div className={css.formGroup}>
                  <label className={css.label}>
                    {t('Фамилия')} <span className={css.required}>*</span>
                  </label>
                  <input
                    type="text"
                    id="lastName"
                    value={formData.lastName}
                    onChange={handleChange}
                    onBlur={() => validateForm()}
                    className={`${css.formControl} ${errors.lastName ? css.error : ''}`}
                    maxLength={50}
                    readOnly
                  />
                  {errors.lastName && <div className={css.errorMessage}>{errors.lastName}</div>}
                </div>
              </div>

              <div className={css.formRow}>
                <div className={css.formGroup}>
                  <label className={css.label}>
                    {t('Email')} <span className={css.required}>*</span>
                  </label>
                  <input
                    type="email"
                    id="email"
                    value={formData.email}
                    onChange={handleEmailChange}
                    onBlur={() => validateForm()}
                    className={`${css.formControl} ${errors.email ? css.error : ''}`}
                    maxLength={255}
                    readOnly
                  />
                  {errors.email && <div className={css.errorMessage}>{errors.email}</div>}
                </div>

                <div className={css.formGroup}>
                  <label className={css.label}>
                    {t('Дата рождения')} <span className={css.required}>*</span>
                  </label>
                  <input
                    type="date"
                    id="birthDate"
                    value={formData.birthDate}
                    onChange={handleBirthDateChange}
                    onBlur={() => validateForm()}
                    className={`${css.formControl} ${errors.birthDate ? css.error : ''}`}
                    readOnly
                  />
                  {errors.birthDate && <div className={css.errorMessage}>{errors.birthDate}</div>}
                </div>
              </div>

              <div className={css.formGroup}>
                <label className={css.label}>
                  {t('Населенный пункт')} <span className={css.required}>*</span>
                </label>
                <input
                  type="text"
                  id="city"
                  value={formData.city}
                  onChange={handleChange}
                  onBlur={() => validateForm()}
                  className={`${css.formControl} ${errors.city ? css.error : ''}`}
                  maxLength={100}
                />
                {errors.city && <div className={css.errorMessage}>{errors.city}</div>}
              </div>

              <div className={css.formGroup}>
                <label className={css.label}>{t('Организация / Учебное учреждение')}</label>
                <input
                  type="text"
                  id="organization"
                  value={formData.organization}
                  onChange={handleChange}
                  className={css.formControl}
                  maxLength={200}
                />
              </div>
            </div>

            <div className={css.formSection}>
              <h2 className={css.sectionTitle}>{t('Состав заявки')}</h2>
              <label className={css.checkboxLabel}>
                <input type="checkbox" checked={isTeam} onChange={event => {
                  setIsTeam(event.target.checked);
                  if (!event.target.checked) {
                    setIsCurator(false);
                    setMembers([]);
                    setErrors(previous => ({ ...previous, members: '' }));
                    updateNominationsByBirthDate(formData.birthDate);
                  }
                }} />
                <span>{t('Командная заявка')}</span>
              </label>

              {isTeam && (
                <>
                  <label className={css.checkboxLabel}>
                    <input type="checkbox" checked={isCurator} onChange={event => {
                      const checked = event.target.checked;
                      setIsCurator(checked);
                      updateNominationsByBirthDate(checked ? (members[0]?.birthDate || '') : formData.birthDate);
                      if (checked) setErrors(previous => ({ ...previous, birthDate: '' }));
                    }} />
                    <span>{t('Я являюсь куратором команды')}</span>
                  </label>
                  <p className={css.fileUploadHint}>{t('Заявкой управляет ваш аккаунт. Если вы участник команды, добавьте остальных участников; если куратор — всех участников.')}</p>
                  {members.map((member, index) => (
                    <div key={index} className={css.memberCard}>
                      <div className={css.formRow}>
                        <div className={css.formGroup}>
                          <label className={css.label}>{t('Имя участника')} *</label>
                          <input className={css.formControl} value={member.firstName} maxLength={50}
                            onChange={event => updateMember(index, 'firstName', event.target.value)} />
                        </div>
                        <div className={css.formGroup}>
                          <label className={css.label}>{t('Фамилия участника')} *</label>
                          <input className={css.formControl} value={member.lastName} maxLength={50}
                            onChange={event => updateMember(index, 'lastName', event.target.value)} />
                        </div>
                      </div>
                      <div className={css.formGroup}>
                        <label className={css.label}>{t('Дата рождения участника')} *</label>
                        <input type="date" className={css.formControl} value={member.birthDate}
                          onChange={event => updateMember(index, 'birthDate', event.target.value)} />
                      </div>
                      {errors[`member-${index}`] && <div className={css.errorMessage}>{errors[`member-${index}`]}</div>}
                      <button type="button" className={css.removeMemberButton}
                        onClick={() => {
                          setMembers(previous => previous.filter((_, i) => i !== index));
                          if (isCurator && index === 0) updateNominationsByBirthDate(members[1]?.birthDate || '');
                        }}>
                        {t('Удалить участника')}
                      </button>
                    </div>
                  ))}
                  {errors.members && <div className={css.errorMessage}>{errors.members}</div>}
                  <button type="button" className={`${css.btn} ${css.btnSecondary}`}
                    onClick={() => setMembers(previous => [...previous, { firstName: '', lastName: '', birthDate: '' }])}>
                    + {t('Добавить участника')}
                  </button>
                </>
              )}
            </div>

            <div className={css.formSection}>
              <h2 className={css.sectionTitle}>{t('Информация о работе')}</h2>

              <div className={css.formGroup}>
                <label className={css.label}>
                  {t('Номинация')} <span className={css.required}>*</span>
                </label>
                <select
                  id="nomination"
                  value={formData.nomination}
                  onChange={handleChange}
                  onBlur={() => validateForm()}
                  className={`${css.formControl} ${css.select} ${errors.nomination ? css.error : ''}`}
                  disabled={availableNominations.length === 0}
                >
                  <option value="">
                    {availableNominations.length === 0
                      ? t('Сначала укажите дату рождения')
                      : t('Выберите номинацию')}
                  </option>
                  {availableNominations.map(nom => (
                    <option key={nom.conId} value={nom.conId}>{t(nom.name)}</option>
                  ))}
                </select>
                {errors.nomination && <div className={css.errorMessage}>{errors.nomination}</div>}
              </div>

              <div className={css.formGroup}>
                <label className={css.label}>
                  {t('Название работы')} <span className={css.required}>*</span>
                </label>
                <input
                  type="text"
                  id="workTitle"
                  value={formData.workTitle}
                  onChange={handleChange}
                  onBlur={() => validateForm()}
                  className={`${css.formControl} ${errors.workTitle ? css.error : ''}`}
                  maxLength={200}
                />
                {errors.workTitle && <div className={css.errorMessage}>{errors.workTitle}</div>}
              </div>

              <div className={css.formGroup}>
                <label className={css.label}>
                  {t('Описание работы')} <span className={css.required}>*</span>
                </label>
                <textarea
                  id="description"
                  value={formData.description}
                  onChange={handleChange}
                  onBlur={() => validateForm()}
                  className={`${css.formControl} ${css.textarea} ${errors.description ? css.error : ''}`}
                  maxLength={500}
                  placeholder={t('Материалы, техника исполнения...')}
                />
                {errors.description && <div className={css.errorMessage}>{errors.description}</div>}
              </div>

              <div className={css.formGroup}>
                <label className={css.label}>{t('ФИО наставника')}</label>
                <input
                  type="text"
                  id="mentorName"
                  value={formData.mentorName}
                  onChange={handleChange}
                  className={css.formControl}
                  maxLength={100}
                />
              </div>
            </div>

            <div className={css.formSection}>
              <h2 className={css.sectionTitle}>{t('Загрузка работ')}</h2>

              {uploadedFiles.length > 0 && (
                <div className={css.fileList}>
                  {uploadedFiles.map((file, index) => (
                    <div key={index} className={css.fileItem}>
                      <span className={css.fileIcon}>📄</span>
                      <div className={css.fileDetails}>
                        <div className={css.fileName}>{file.name}</div>
                        <div className={css.fileSize}>{formatFileSize(file.size)}</div>
                      </div>
                      <span className={css.fileRemove} onClick={() => removeFile(index)}>✕</span>
                    </div>
                  ))}
                </div>
              )}

              {isUploading && (
                <div className={css.progressBar}>
                  <div
                    className={css.progressFill}
                    style={{ width: `${uploadProgress}%` }}
                  />
                  <span>{uploadProgress}%</span>
                </div>
              )}

              <div className={css.fileUpload} onClick={() => document.getElementById('workFiles')?.click()}>
                <input
                  type="file"
                  id="workFiles"
                  accept=".jpg,.jpeg,.png,.tif,.tiff"
                  style={{ display: 'none' }}
                  multiple
                  onChange={handleFileSelect}
                  disabled={isUploading}
                />
                <div className={css.fileUploadIcon}>📁</div>
                <div className={css.fileUploadText}>{t('Нажмите для выбора файлов')}</div>
                <div className={css.fileUploadHint}>{t('PNG, JPG, TIFF до 20 МБ; всего до 10 файлов и 20 МБ на заявку')}</div>
              </div>
              {errors.files && <div className={css.errorMessage}>{errors.files}</div>}
            </div>

            <div className={css.formActions}>
              <button type="button" className={`${css.btn} ${css.btnSecondary}`} onClick={handleClear} disabled={isUploading}>
                {t('Очистить')}
              </button>
              <button type="submit" className={`${css.btn} ${css.btnPrimary}`} disabled={isUploading || submitting}>
                {isUploading ? t('Загрузка...') : t('Отправить заявку')}
              </button>
            </div>
          </form>
        </div>
      </div>

      <Notification
        message={notification.message}
        type={notification.type}
        isVisible={notification.show}
        onClose={() => setNotification(prev => ({ ...prev, show: false }))}
      />
    </>
  );
};
