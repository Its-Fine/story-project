import React from 'react';
import { DecorLines } from '../../components/Layout/DecorLines';

export const AdminPage = () => {
  return (
    <>
      <DecorLines />
      <div style={{ padding: '24px', maxWidth: '900px', margin: '0 auto' }}>
        <h1 style={{ fontSize: '24px', marginBottom: '16px' }}>
          Административная панель
        </h1>
        <p style={{ color: '#666' }}>
          Здесь будет управление пользователями, заявками и конкурсами.
          Доступ к этому разделу имеют только пользователи с ролью
          <strong>ADMIN</strong>.
        </p>
      </div>
    </>
  );
};
