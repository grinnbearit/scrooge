import re
import pandas as pd
import edn_format as edn
from datetime import datetime


AMOUNT_RE = r"[-\d,\.]+"
def is_amount(s):
    """
    returns True if s is a number (with commas and periods)
    """
    return re.match(AMOUNT_RE, s) is not None


def parse_amount(s):
    """
    returns a python double after stripping out commas
    """
    return float(s.replace(",", ""))


PRICE_RE = r"([^\s]+) ([^\s]+)( \{([^\s]+) ([^\s]+)\}.+)?"
def parse_posting(pstng):
    """
    parses an edn posting into a dict
    """
    account = pstng[1]
    amount_str = pstng[2]
    note = pstng[4] if len(pstng) == 5 else None
    (amt_1, amt_2, _, price_1, price_2) = re.match(PRICE_RE, amount_str).groups()

    if is_amount(amt_1):
        (amount, commodity) = (parse_amount(amt_1), amt_2)
    else:
        (amount, commodity) = (parse_amount(amt_2), amt_1)

    if price_1 is not None:
        if is_amount(price_1):
            (price, unit) = (parse_amount(price_1), price_2)
        else:
            (price, unit) = (parse_amount(price_2), price_1)
    else:
        (price, unit) = (None, None)

    return {"account": account,
            "amount": amount,
            "commodity": commodity,
            "price": price,
            "unit": unit,
            "note": note}


def parse_transaction(txn):
    """
    parses an edn transaction into a dict
    """
    (msb, lsb, _) = txn[2]
    date = datetime.fromtimestamp((msb << 16) + lsb).date()
    payee = txn[4]
    postings = [parse_posting(pstng) for pstng in txn[5:]]
    return {"date": date,
            "payee": payee,
            "postings": postings}


def parse_ledger(filename):
    """
    returns a parsed ledger file as a pandas dataframe
    """
    acc = []
    with open(filename) as f:
        for (idx, txn) in enumerate(edn.loads(f.read())):
            transaction = parse_transaction(txn)
            for posting in transaction["postings"]:
                posting["txid"] = idx
                posting["date"] = transaction["date"]
                posting["payee"] = transaction["payee"]
                acc.append(posting)
    return pd.DataFrame(acc).set_index("txid")
