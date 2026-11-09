import json
import os

from openpyxl import load_workbook
from openpyxl.worksheet.worksheet import Worksheet

import utilities.file as file
from diploma.configs import *
from diploma.prettifier import *


def load_cell(ws: Worksheet, row, column, strip=True) -> str | None:
    if strip:
        return pretty_text(ws.cell(row=row, column=column).value) if ws.cell(row=row, column=column).value else None
    return ws.cell(row=row, column=column).value


def names_parser(ws: Worksheet, row, column) -> str:
    names = re.split(r'[,;.]', load_cell(ws, row, column, False))
    for name_ind in range(len(names)):
        names[name_ind] = pretty_text(re.split(r'\d', names[name_ind])[0])
    return ',\n'.join(names)
    # return names


def prize_parser(ws: Worksheet, row, column) -> str:
    if load_cell(ws, row, column, False):
        place = str(load_cell(ws, row, column, False)).strip()
        if place in ['P', '1', '2', '3', 'X']: return place
    return '00'


def load_sheet(ws: Worksheet):
    row, name_2, result = 3, None, []

    while True:
        if not load_cell(ws, row, 5, False):
            break

        if load_cell(ws, row, 2, False):
            name_2 = load_cell(ws, row, 2)

        names = names_parser(ws, row, 10) if load_cell(ws, row, 10, False) else name_2  # [name_2]
        work_name = replace_quotes_nested_type2(load_cell(ws, row, 5))
        mentor_name = load_cell(ws, row, 9)
        place = replace_quotes_nested_type2(load_cell(ws, row, 8))
        prize = prize_parser(ws, row, 17)

        # for name in names:
        #     result.append({"WorkName": work_name, "MentorName": mentor_name, "Name": name, "Place": place, "Prize": prize})
        result.append({"WorkName": work_name, "MentorName": mentor_name, "Name": names, "Place": place, "Prize": prize})
        row += 1

    return result


def load_table(load_filename: str):
    wb = load_workbook(filename=load_filename, read_only=True)
    result = {}
    for i in LIST_NAMES:
        age_name = REAL_NAMES[i]
        result[age_name] = load_sheet(wb[i])
    wb.close()
    return result


if __name__ == '__main__':
    result_full = {}
    for filename in os.listdir('../temp_new/works'):
        if filename.endswith('.xlsx'):
            result_full[filename[:-5]] = load_table(f'../temp_new/works/{filename}')
    file.save('../temp_new/result_4.json', json.dumps(result_full, ensure_ascii=False, indent=4), encoding='utf-8')
