import re


def pretty_text(text: str):
    return ' '.join([i for i in re.split(r'\s', text) if len(i)])


def replace_quotes_nested_type1(text):
    """
    Вариант 1: Вложенные кавычки по типу « « « »»»
    (открывающие и закрывающие кавычки накапливаются)
    """
    if text is None:
        return None

    result = []
    quote_stack = []

    for char in text:
        if char in '"\'':
            if quote_stack and quote_stack[-1] == 'open':
                result.append('»')
                quote_stack.pop()
            else:
                result.append('«')
                quote_stack.append('open')
        else:
            result.append(char)

    return ''.join(result)


def replace_quotes_nested_type2(text):
    """
    Вариант 2: Вложенные кавычки по типу « »« »« »
    (чередование: открывающая, закрывающая, открывающая, закрывающая)
    """
    if text is None:
        return None

    result = []
    is_open = False

    for char in text:
        if char in '"\'':
            if is_open:
                result.append('»')
                is_open = False
            else:
                result.append('«')
                is_open = True
        else:
            result.append(char)

    return ''.join(result)