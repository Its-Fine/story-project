import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { DecorLines } from '../../components/Layout/DecorLines';
import { Notification } from '../../components/UI/Notification';
import { applicationsApi } from '../../api/applications';
import { storageApi } from '../../api/storage';
import { useLanguage } from '../../contexts/LanguageContext';
import css from './index.module.scss';

interface Nomination {
  id: number;
  name: string;
}

export const ApplicationPage = () => {
  const { contestId } = useParams<{ contestId: string }>();
  const navigate = useNavigate();
  const { t, language } = useLanguage();

  const [formData, setFormData] = useState({
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

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [isUploading, setIsUploading] = useState(false);
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
    }
  }, [navigate]);

  const nominationsByAge = {
    junior: {
      min: 10, max: 13,
      list: [
        { id: 1, name: 'Графический дизайн (плакат, календарь, открытка, упаковка и др.)' },
        { id: 2, name: 'Иллюстрация (авторская графика, выполненная различными художественными материалами)' },
        { id: 3, name: 'Арт-дизайн (проекты, соединяющие в себе принципы дизайн-проектирования и традиционного художественного творчества)' },
        { id: 4, name: 'Туристический сувенир (современное прочтение традиционных ремесел и народных промыслов в сувенирной продукции)' }
      ]
    },
    middle: {
      min: 14, max: 17,
      list: [
        { id: 5, name: 'Дизайн костюма (коллекции авторской одежды)' },
        { id: 6, name: 'Графический дизайн (плакат, календарь, открытка, упаковка и др.)' },
        { id: 7, name: 'Иллюстрация (авторская графика, выполненная различными художественными материалами)' },
        { id: 8, name: 'Арт-дизайн (проекты, соединяющие в себе принципы дизайн-проектирования и традиционного художественного творчества)' },
        { id: 9, name: 'Туристический сувенир (современное прочтение традиционных ремесел и народных промыслов в сувенирной продукции)' },
        { id: 10, name: 'Диджитал-арт и цифровая живопись' }
      ]
    },
    senior: {
      min: 18, max: 35,
      list: [
        { id: 11, name: 'Дизайн костюма (коллекции авторской одежды эскизы, макеты и/или готовые изделия одежды, обуви, аксессуаров)' },
        { id: 12, name: 'Графический дизайн (плакат, календарь, открытка, упаковка и др.)' },
        { id: 13, name: 'Иллюстрация (авторская графика, выполненная различными художественными материалами)' },
        { id: 14, name: 'Арт-дизайн (проекты, соединяющие в себе принципы дизайн-проектирования и традиционного художественного творчества)' },
        { id: 15, name: 'Декоративно-прикладное искусство (сохранение народных традиций, творческое осмысление, поиск современных форм и их интерпретация)' },
        { id: 16, name: 'Туристический сувенир (современное прочтение традиционных ремесел и народных промыслов в сувенирной продукции)' },
        { id: 17, name: 'Диджитал-арт и цифровая живопись (художественно-графические произведения, выполненные с помощью компьютерных технологий и специализированного программного обеспечения)' },
        { id: 18, name: 'Промышленный дизайн (эскизы, макеты, действующие прототипы промышленных изделий: техника, оборудование, предметы интерьера и городской среды, игрушки, ювелирные изделия, сувенирная продукция и др.)' }
      ]
    }
  };

  const [availableNominations, setAvailableNominations] = useState<Nomination[]>([]);

  const calculateAge = (birthDate: string): number => {
    const birth = new Date(birthDate);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  };

  const validateEmail = (email: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  };

  const validateAge = (birthDate: string): { isValid: boolean; message: string } => {
    if (!birthDate) return { isValid: false, message: t('Введите дату рождения') };
    const age = calculateAge(birthDate);
    if (age < 10) return { isValid: false, message: t('Возраст должен быть от 10 лет') };
    if (age > 35) return { isValid: false, message: t('Возраст должен быть до 35 лет') };
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

  const handleBirthDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const date = e.target.value;
    setFormData(prev => ({ ...prev, birthDate: date }));
    const ageValidation = validateAge(date);
    if (date && !ageValidation.isValid) {
      setErrors(prev => ({ ...prev, birthDate: ageValidation.message }));
    } else {
      setErrors(prev => ({ ...prev, birthDate: '' }));
    }
    if (date) {
      const age = calculateAge(date);
      let group = null;
      if (age >= 10 && age <= 13) group = nominationsByAge.junior;
      else if (age >= 14 && age <= 17) group = nominationsByAge.middle;
      else if (age >= 18 && age <= 35) group = nominationsByAge.senior;
      if (group) {
        setAvailableNominations(group.list);
      } else {
        setAvailableNominations([]);
      }
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files) {
      if (uploadedFiles.length + files.length > 3) {
        showNotification(t('Можно загрузить не более 3 файлов'), 'error');
        return;
      }
      const newFiles = Array.from(files).filter(file => file.size <= 10 * 1024 * 1024);
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

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};
    let isValid = true;

    const required = ['firstName', 'lastName', 'email', 'birthDate', 'city', 'nomination', 'workTitle', 'description'];

    required.forEach(field => {
      if (!formData[field as keyof typeof formData]) {
        newErrors[field] = t('Заполните поле');
        isValid = false;
      }
    });

    if (formData.email && !validateEmail(formData.email)) {
      newErrors.email = t('Введите корректный email');
      isValid = false;
    }

    if (formData.birthDate) {
      const ageValidation = validateAge(formData.birthDate);
      if (!ageValidation.isValid) {
        newErrors.birthDate = ageValidation.message;
        isValid = false;
      }
    }

    if (uploadedFiles.length === 0) {
      newErrors.files = t('Загрузите файл работы');
      isValid = false;
    } else if (uploadedFiles.length > 3) {
      newErrors.files = t('Можно загрузить не более 3 файлов');
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

    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
      return;
    }

    const nominationName = availableNominations.find(
      nomination => nomination.id === Number(formData.nomination)
    )?.name;
    const response = await applicationsApi.createApplication(
      {
        name: formData.workTitle,
        description: nominationName
          ? `Номинация: ${nominationName}\n\n${formData.description}`
          : formData.description,
        city: formData.city,
        organization: formData.organization || undefined,
        mentorName: formData.mentorName || undefined,
        members: [],
        isTeam: false,
        isCurator: false,
      },
      contestId || '',
      token
    );

    const appId = response.data?.appId;
    if (response.status !== 201 || !appId) {
      showNotification(response.error?.message || t('Не удалось создать заявку'), 'error');
      return;
    }

    showNotification(t('Заявка создана. Загружаем файлы...'), 'success');
    const filesUploaded = await uploadFiles(appId);
    if (!filesUploaded) {
      await applicationsApi.deleteApplication(appId, token);
      showNotification(t('Файл не загружен, черновик заявки удалён. Проверьте формат и повторите.'), 'error');
      return;
    }

    showNotification(t('Заявка отправлена!'), 'success');
    setTimeout(() => navigate('/profile'), 1500);
  };

  const handleClear = () => {
    setFormData({
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
    setUploadedFiles([]);
    setErrors({});
    setAvailableNominations([]);
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
                    <option key={nom.id} value={nom.id}>{nom.name}</option>
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
                <div className={css.fileUploadHint}>{t('PNG, JPG, TIFF до 10 МБ')}</div>
              </div>
              {errors.files && <div className={css.errorMessage}>{errors.files}</div>}
            </div>

            <div className={css.formActions}>
              <button type="button" className={`${css.btn} ${css.btnSecondary}`} onClick={handleClear} disabled={isUploading}>
                {t('Очистить')}
              </button>
              <button type="submit" className={`${css.btn} ${css.btnPrimary}`} disabled={isUploading}>
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