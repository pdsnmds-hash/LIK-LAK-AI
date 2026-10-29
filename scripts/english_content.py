"""English-only publication boundary; stable legacy field names remain for compatibility."""
import re


def english_only(value):
    if isinstance(value, list):
        return [english_only(item) for item in value]
    if isinstance(value, dict):
        selected = dict(value)
        for key in selected:
            other = key[:-3] + '_en' if key.endswith('_bg') else ('en' if key == 'bg' else None)
            if other in value:
                selected[key] = value[other]
        if 'prompt_bg' in selected and 'prompt' in selected:
            selected['prompt_bg'] = selected['prompt']
        result = {key: english_only(item) for key, item in selected.items()}
        return result
    if isinstance(value, str):
        value = value.replace('Здравейте {name}, имате задачи.', 'Hello {name}, you have tasks.')
        value = value.replace('BG/EN', 'English').replace('BG / EN', 'English')
        value = value.replace('Bulgarian or English', 'English').replace('Bulgarian and English', 'English')
        value = value.replace('Bulgarian text', 'Unicode text').replace('Bulgarian vocabulary', 'additional vocabulary')
        value = value.replace('Bulgarian terms', 'additional terms').replace('Bulgarian messages', 'Messages outside the vocabulary').replace('Bulgarian', 'additional-language')
        if re.search('[\u0400-\u04ff]', value):
            raise ValueError('Non-English publication text requires review: ' + value[:120])
        return value
    return value
