from openpyxl import Workbook
from openpyxl.drawing.spreadsheet_drawing import TwoCellAnchor, AnchorMarker
from openpyxl.drawing.image import Image as XLImage
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side
from openpyxl.utils import column_index_from_string, get_column_letter

from compiler.images import load_compressed_image
from compiler.headers import HEADERS


THIN_BORDER = Side(style='thin', color='000000')
BOLD_BORDER = Side(style='thick', color='000000')


TABLE_PLACE = (1, 18)
USER_PLACE = (1, 4)
APP_PLACE = (4, 11)
WORK_PLACE = (11, 16)


def add_images_row(ws, works, row, col_0, col_1, images_path, yandex_link):
    if len(works) > col_1 - col_0:
        print("Много работ")
        ws.cell(row=row, column=col_1, value=f"{yandex_link}{'/'.join(works[0]["fileName"].split('/')[0:-1])}")
        ws.cell(row=row, column=col_0, value="Много работ")
        for col in range(col_0, col_1):
            ws.cell(row=row, column=col).fill = PatternFill(start_color="FFFF00", end_color="FFFF00", fill_type="solid")
        return False

    for i, work in enumerate(works, start=col_0):
        try:
            xl_img = XLImage(load_compressed_image(f"{images_path}/{work["fileName"]}"))
            xl_img.anchor = TwoCellAnchor(editAs="twoCell", _from=AnchorMarker(col=i - 1, row=row - 1), to=AnchorMarker(col=i, row=row))
            ws.add_image(xl_img)
            print(f"Загружено изображение {work["name"]}")

        except Exception as e:
            ws.cell(row=row, column=i, value="Ошибка")
            ws.cell(row=row, column=i).fill = PatternFill(start_color="FF0000", end_color="FF0000", fill_type="solid")
            print(f"Ошибка загрузки {work["name"]}: {e}")

    ws.cell(row=row, column=col_1, value=f"{yandex_link}{'/'.join(works[0]["fileName"].split('/')[0:-1])}")
    return True


def create_sheet(data, wb, ws_name, images_path, yandex_link):
    ws = wb.create_sheet(ws_name)

    for header in HEADERS:
        text = header.get("text")
        cell = header.get("cell")
        cell_end = header.get("cellEnd")
        width = header.get("width")

        ws[cell] = text
        if cell_end: ws.merge_cells(f"{cell}:{cell_end}")

        if width:
            col = column_index_from_string(''.join([c for c in cell if c.isalpha()]))

            if cell_end:
                end_col = column_index_from_string(''.join([c for c in cell_end if c.isalpha()]))
                for col_num in range(col, end_col + 1):
                    col_letter = get_column_letter(col_num)
                    ws.column_dimensions[col_letter].width = width
            else:
                ws.column_dimensions[get_column_letter(col)].width = width

        cell_obj = ws[cell]
        cell_obj.font = Font(bold=True)

    for col in range(*TABLE_PLACE):
        ws.cell(row=1, column=col).border = Border(THIN_BORDER, THIN_BORDER, THIN_BORDER, THIN_BORDER)
        ws.cell(row=2, column=col).border = Border(THIN_BORDER, THIN_BORDER, THIN_BORDER, THIN_BORDER)

    # --

    row = 3
    for uid, user in enumerate(data):
        for col in range(*TABLE_PLACE):
            ws.cell(row=row, column=col).border = Border(THIN_BORDER, THIN_BORDER, BOLD_BORDER, THIN_BORDER)

        print(f"Начало формирования пользователя на строке {row}")
        ws.cell(row=row, column=1, value=user["email"])
        ws.cell(row=row, column=2, value=f"{user["firstName"]} {user["lastName"]}")
        ws.cell(row=row, column=3, value=user["birthDate"])

        old_row = row

        if len(user["applications"]):
            for app in user["applications"]:
                print(f"Начало формирования заявки на строке {row}")
                ws.cell(row=row, column=4, value=app["appId"])
                ws.cell(row=row, column=5, value=app["name"])
                ws.cell(row=row, column=6, value=app["description"])
                ws.cell(row=row, column=7, value=app["city"])
                ws.cell(row=row, column=8, value=app["organization"])
                ws.cell(row=row, column=9, value=app["mentorName"])

                members = ";\n".join([f"{i["firstName"]} {i["lastName"]} {i["birthDate"]}" for i in app["members"]])
                ws.cell(row=row, column=10, value=members)

                if old_row < row:
                    for col in range(*TABLE_PLACE):
                        ws.cell(row=row, column=col).border = Border(THIN_BORDER, THIN_BORDER, THIN_BORDER, THIN_BORDER)

                if uid % 2:
                    for col in range(*TABLE_PLACE):
                        ws.cell(row=row, column=col).fill = PatternFill(start_color="F5F5F5", end_color="F5F5F5",
                                                                        fill_type="solid")

                if app["isTeam"]:
                    ws.cell(row=row, column=10).fill = PatternFill(start_color="00FFFF", end_color="00FFFF",
                                                                   fill_type="solid")

                if app["isCurator"]:
                    ws.cell(row=row, column=10).fill = PatternFill(start_color="FF69B4", end_color="FF69B4",
                                                                   fill_type="solid")

                if len(app["works"]): add_images_row(ws, app["works"], row, WORK_PLACE[0], WORK_PLACE[1], images_path,
                                                     yandex_link)

                row += 1
                print(f"Завершение формирования заявки на строке {row}")
        else:
            print("Нет заявок")
            ws.cell(row=row, column=APP_PLACE[0], value="Нет заявок")
            for col in range(*TABLE_PLACE):
                ws.cell(row=row, column=col).fill = PatternFill(start_color="FF0000", end_color="FF0000",
                                                                fill_type="solid")
            row += 1

        if row - 1 > old_row:
            ws.merge_cells(f"A{old_row}:A{row - 1}")
            ws.merge_cells(f"B{old_row}:B{row - 1}")
            ws.merge_cells(f"C{old_row}:C{row - 1}")

        print(f"Завершение формирования пользователя на строке {row}")

    # --

    for row in range(3, ws.max_row + 1):
        for column in range(1, ws.max_column + 1):
            ws.cell(row, column).alignment = Alignment(wrap_text=True, vertical='top')
        if ws.row_dimensions[row].height is None or ws.row_dimensions[row].height < 100:
            ws.row_dimensions[row].height = 150


def create_table(data, save_filename, images_path, yandex_link):
    wb = Workbook()

    for i in data:
        create_sheet(data[i], wb, i, images_path, yandex_link)

    wb.save(save_filename)
