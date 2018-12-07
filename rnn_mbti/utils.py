import re


# Function to clean data ... will be useful later
def post_cleaner(post):
    """cleans individual posts`.
    Args:
        post-string
    Returns:
         cleaned up post`.
    """
    # Covert all uppercase characters to lower case
    post = post.lower()

    # Remove |||
    post = post.replace('|||', " ")

    # Remove URLs, links etc
    post = re.sub(
        r'''(?i)\b((?:https?://|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’]))''',
        '', post, flags=re.MULTILINE)
    # This would have removed most of the links but probably not all

    # Remove puntuations
    puncs1 = ['@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=', '{', '}', '[', ']', '|', '\\', '"', "'",
              ';', ':', '<', '>', '/']
    for punc in puncs1:
        post = post.replace(punc, '')

    puncs2 = [',', '.', '?', '!', '\n']
    for punc in puncs2:
        post = post.replace(punc, ' ')
        # Remove extra white spaces
    post = re.sub('\s+', ' ', post).strip()
    return post


def list_to_well_formed_string(data):
    return '\n'.join(str(b) for b in data)

