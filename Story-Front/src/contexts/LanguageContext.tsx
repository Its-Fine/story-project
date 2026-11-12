import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';

export type Language = 'ru' | 'en';

const english: Record<string, string> = {
  'Вход': 'Sign in',
  'Регистрация': 'Sign up',
  'Личный кабинет': 'Personal account',
  'Международный творческий конкурс': 'International creative contest',
  'Email': 'Email',
  'Пароль': 'Password',
  'Забыли пароль?': 'Forgot password?',
  'Войти': 'Sign in',
  'Вход...': 'Signing in...',
  'Нет аккаунта?': "Don't have an account?",
  'Зарегистрироваться': 'Create an account',
  'Уже есть аккаунт?': 'Already have an account?',
  'Имя': 'First name',
  'Фамилия': 'Last name',
  'Дата рождения': 'Date of birth',
  'Подтверждение': 'Confirm password',
  'Регистрация...': 'Creating account...',
  'Родитель или законный представитель дал согласие на регистрацию участника': 'A parent or legal guardian has given consent to register the participant',
  'Обязательные поля': 'Required fields',
  'Введите email': 'Enter your email',
  'Введите корректный email': 'Enter a valid email address',
  'Введите пароль': 'Enter your password',
  'Введите имя': 'Enter your first name',
  'Введите фамилию': 'Enter your last name',
  'Введите дату рождения': 'Enter your date of birth',
  'Возраст должен быть от 1 до 100 лет': 'Age must be between 1 and 100',
  'Пароль должен быть не менее 8 символов': 'Password must contain at least 8 characters',
  'Подтвердите пароль': 'Confirm your password',
  'Пароли не совпадают': 'Passwords do not match',
  'Для регистрации до 18 лет необходимо согласие родителя': 'Parental consent is required for participants under 18',
  'Вход выполнен успешно!': 'Signed in successfully!',
  'Регистрация прошла успешно!': 'Registration successful!',
  'Ошибка входа. Проверьте email и пароль': 'Sign-in failed. Check your email and password',
  'Функция восстановления пароля будет доступна позже': 'Password recovery will be available soon',
  'Этот email уже зарегистрирован. Перейдите во вкладку «Вход».': 'This email is already registered. Switch to the sign-in tab.',
  'Для участника младше 18 лет требуется согласие родителя.': 'Parental consent is required for participants under 18.',
  'Не удалось зарегистрироваться. Проверьте данные и попробуйте ещё раз.': 'Could not create the account. Check your details and try again.',
  'Личный кабинет': 'Personal account',
  'Подать заявку': 'Submit application',
  '+ Подать заявку': '+ Submit application',
  'Выйти': 'Log out',
  'Пользователь': 'User',
  'Редактировать профиль': 'Edit profile',
  'Редактирование профиля': 'Edit profile',
  'Сохранить': 'Save',
  'Отмена': 'Cancel',
  'Загрузка...': 'Loading...',
  'Всего заявок': 'Total applications',
  'На рассмотрении': 'Under review',
  'Принято': 'Accepted',
  'Отклонено': 'Rejected',
  'Мои заявки': 'My applications',
  'Все заявки': 'All applications',
  'Принятые': 'Accepted',
  'Отклоненные': 'Rejected',
  'У вас пока нет заявок': 'You have no applications yet',
  'По выбранному фильтру заявок нет': 'No applications match this filter',
  'Список конкурсов пока не загружен. Обновите страницу через несколько секунд.': 'The contest list has not loaded yet. Refresh the page in a few seconds.',
  'Сессия истекла. Выйдите и войдите снова.': 'Your session has expired. Sign out and sign in again.',
  'У этой учётной записи нет доступа к заявкам участника.': 'This account does not have access to participant applications.',
  'Ошибка при загрузке заявок': 'Could not load applications',
  'Ошибка при удалении заявки': 'Could not delete the application',
  'Вы уверены, что хотите удалить заявку?': 'Are you sure you want to delete this application?',
  'Подать первую заявку': 'Submit your first application',
  'Причина отказа:': 'Rejection reason:',
  'Работа:': 'Work:',
  'Описание:': 'Description:',
  'Файлов:': 'Files:',
  'Редактировать': 'Edit',
  'Удалить': 'Delete',
  'Админка': 'Admin',
  'Роль:': 'Role:',
  'Участник': 'Participant',
  'Администратор': 'Administrator',
  'Панель администратора': 'Administrator dashboard',
  'УПРАВЛЕНИЕ КОНКУРСОМ': 'CONTEST MANAGEMENT',
  'Участники, заявки, работы и настройки приёма.': 'Participants, applications, submitted works and registration settings.',
  'Скачать Excel-отчёт': 'Download Excel report',
  'Пользователи': 'Users',
  'Заявки': 'Applications',
  'Приём заявок': 'Applications',
  'Закрыт': 'Closed',
  'Открыт': 'Open',
  'Открыть приём': 'Open applications',
  'Закрыть приём': 'Close applications',
  'Обзор': 'Overview',
  'Участники': 'Participants',
  'Обновить': 'Refresh',
  'Последние заявки': 'Latest applications',
  'Первая страница списка — до 100 заявок.': 'The first page contains up to 100 applications.',
  'Все заявки →': 'All applications →',
  'Пока нет заявок.': 'No applications yet.',
  'Доступны просмотр заявок и работ, выгрузка Excel и управление приёмом. Оценки и смены статуса заявок в backend пока нет.': 'You can review applications and submitted works, export Excel reports and manage registration. Rating and application status changes are not available in the backend yet.',
  'Заявки участников': 'Participant applications',
  'Для заявки можно посмотреть работы и скачать файлы.': 'Open an application to view and download submitted files.',
  'из': 'of',
  'Заявок пока нет.': 'There are no applications yet.',
  'Скрыть работы': 'Hide works',
  'Работы': 'Works',
  'Город:': 'City:',
  'Формат:': 'Format:',
  'Команда': 'Team',
  'Индивидуально': 'Individual',
  'Организация:': 'Organization:',
  'Наставник:': 'Mentor:',
  'Загрузка файлов…': 'Loading files…',
  'В этой заявке нет файлов.': 'This application has no files.',
  'Поиск по email, роль и количество заявок.': 'Search by email, role and application count.',
  'показано': 'shown',
  'всего': 'total',
  'Поиск по email': 'Search by email',
  'Найти': 'Search',
  'Сбросить': 'Reset',
  'Пользователи не найдены.': 'No users found.',
  'заявок': 'applications',
  'Excel-отчёт скачан.': 'Excel report downloaded.',
  'Приём заявок закрыт.': 'Applications are closed.',
  'Приём заявок открыт.': 'Applications are open.',
  'Не удалось загрузить данные панели': 'Could not load dashboard data',
  'Не удалось загрузить пользователей': 'Could not load users',
  'Не удалось изменить статус конкурса': 'Could not update contest status',
  'Не удалось загрузить файлы': 'Could not load files',
  'Ошибка скачивания файла': 'Could not download file',
  'Ошибка скачивания отчёта': 'Could not download the report',
  'Сессия администратора отсутствует. Войдите снова.': 'Administrator session is missing. Please sign in again.',
};

interface LanguageContextValue {
  language: Language;
  setLanguage: (language: Language) => void;
  t: (text: string) => string;
}

const LanguageContext = createContext<LanguageContextValue | undefined>(undefined);

export const LanguageProvider = ({ children }: { children: React.ReactNode }) => {
  const [language, setLanguage] = useState<Language>(() => {
    return localStorage.getItem('language') === 'en' ? 'en' : 'ru';
  });

  useEffect(() => {
    localStorage.setItem('language', language);
    document.documentElement.lang = language;
  }, [language]);

  const value = useMemo<LanguageContextValue>(() => ({
    language,
    setLanguage,
    t: (text: string) => language === 'en' ? english[text] ?? text : text,
  }), [language]);

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>;
};

export const useLanguage = () => {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used inside LanguageProvider');
  }
  return context;
};
