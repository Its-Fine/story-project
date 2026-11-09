import os
import re
import json

from docxtpl import DocxTemplate
from docx2pdf import convert

import utilities.file as file


def create_diploma(data, output_dir, certificates=True):
    """
    Создаёт дипломы из шаблонов Word с заменой полей.

    data: словарь с данными студента
    output_dir: папка для сохранения
    certificates: переключатель на выпуск сертификатов или дипломов
    """
    for nomination in data:
        for category in data[nomination]:
            os.makedirs(save_path := str(os.path.join(output_dir, nomination, category)), exist_ok=True)
            for diploma in data[nomination][category]:
                try:
                    if certificates:
                        template = DocxTemplate(f'./templates/00.docx')
                    elif diploma['Prize'] not in ['00', 'I00']:
                        template = DocxTemplate(f'./templates/{diploma['Prize']}.docx')
                    else:
                        continue

                    diploma_data = {'Nomination': nomination, 'Name': diploma['Name']}
                    if diploma['WorkName'] is not None and diploma['WorkName'] != '':
                        diploma_data['WorkName'] = f'проект «{diploma['WorkName']}»'
                    if diploma['Place'] is not None and diploma['Place'] != '':
                        diploma_data['Place'] = diploma['Place']
                    if diploma['MentorName'] is not None and diploma['MentorName'] != '':
                        diploma_data['MentorName'] = f'наставник: {diploma['MentorName']}'
                    if category != 'NONE':
                        diploma_data['Category'] = category
                    template.render(diploma_data)

                    docx_name = f"{re.sub(r'[?*:/\\\"<>|]', '_', diploma['WorkName'])} - {diploma['Name'].replace('\n', ' ')}.docx"
                    docx_path = os.path.join(save_path, docx_name)
                    template.save(docx_path)

                    print(f"✅ DOCX создан: {docx_path}")

                except Exception:
                    print(f'Unsupported Prize: {diploma["Prize"]}')


def diploma_to_pdf(source_dir, output_dir):
    """
    преобразует Word дипломы в PDF.

    source_dir: папка документов для преобразования
    output_dir: папка для сохранения
    """
    for item in os.listdir(source_dir):
        item_path = os.path.join(source_dir, item)
        if os.path.isdir(item_path):
            diploma_to_pdf(item_path, f'{output_dir}/{item}')
            continue

        os.makedirs(output_dir, exist_ok=True)
        pdf_path = f'{output_dir}/{item.replace('.docx', '.pdf')}'
        convert(item_path, pdf_path)
        print(f"✅ PDF создан: {pdf_path}")


if __name__ == '__main__':
    # create_diploma(json.loads(file.load('../temp/result_6.json', encoding='utf-8')), '../temp/certificates_word', True)
    # diploma_to_pdf('../temp/certificates_word', '../temp/certificates_pdf')
    #
    # create_diploma(json.loads(file.load('../temp/result_6.json', encoding='utf-8')), '../temp/diplomas_word', False)
    # diploma_to_pdf('../temp/diplomas_word', '../temp/diplomas_pdf')
    pass
