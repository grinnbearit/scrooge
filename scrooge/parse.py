import re
import copy
import pandas as pd
import edn_format as edn
from datetime import datetime
from scrooge.constants import EUR, INR


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
                row = copy.deepcopy(posting)
                row["txid"] = idx
                row["date"] = transaction["date"]
                row["payee"] = transaction["payee"]
                acc.append(row)
    return pd.DataFrame(acc).set_index("txid")


def to_dollarmap(pricedb):
    """
    given a list of (commodity, unit, price) returns
    a dict of (commodity, dollar_value)
    """
    acc = {"$": 1.0}
    queue = copy.deepcopy(pricedb)
    while queue:
        (commodity, unit, price) = queue.pop(0)
        if commodity in acc and unit in acc:
            continue
        elif commodity in acc:
            acc[unit] = acc[commodity]/price
        elif unit in acc:
            acc[commodity] = acc[unit] * price
        else:
            acc.append((commodity, unit, price))
    return acc


def parse_pricedb(filename):
    """
    returns a price dataframe of commodity->dollar value
    """
    acc = []
    with open(filename) as f:
        for line in f.readlines():
            (commodity, unit, price) = line.split(" ")[3:]
            acc.append((commodity, unit, float(price)))

    dollarmap = to_dollarmap(acc)
    return (pd.DataFrame(dollarmap.items(), columns=["commodity", "value"])
            .set_index("commodity"))



def parse_historical(filename):
    """
    returns a dataframe of (date, price) with linearly
    interpolated prices for missing dates.

    historical tsv format from investing.com
    """
    df = (pd.read_csv(filename, sep="\t", parse_dates=[0],
                      usecols=["date", "price"], header=0,
                      names=["date", "price"], thousands=",")
          .set_index("date")
          .resample("1D")
          .interpolate())
    return df


def parse_commodity(filename, commodity, unit):
    """
    returns a dataframe of (date, commodity, unit, price)
    uses `parse_historical`
    """
    df = (parse_historical(filename)
          .assign(commodity=commodity,
                  unit=unit)
          [["commodity", "unit", "price"]])
    return df


def parse_historical_prices():
    """
    returns a dataframe of (date, commodity, price) where prices
    are in USD on that date
    """
    df = pd.concat([parse_commodity("historical/eur_usd.tsv", EUR, "$"),
                    parse_commodity("historical/btc_usd.tsv", "BTC", "$"),
                    parse_commodity("historical/usd_inr.tsv", "$", INR)])

    acc = []
    for idx in df.index:
        row = df.loc[idx].values
        dm = to_dollarmap(list(row))
        acc.extend([(idx, c, v) for (c, v) in dm.items()])

    return (pd.DataFrame(acc, columns=["date", "commodity", "value"])
            .set_index(["date", "commodity"]))


def parse_yield(filename, commodity):
    """
    returns a dataframe of (date, commodity, yield)
    uses `parse_historical`
    """
    df = (parse_historical(filename)
          .assign(commodity=commodity)
          .rename(columns={"price": "yield"})
          [["commodity", "yield"]])
    return df


def parse_historical_yield():
    """
    returns a dataframe of (date, commodity, yield) where yield
    is the annualised return on that date
    """
    df = pd.concat([parse_yield("historical/eur_3my.tsv", EUR),
                    parse_yield("historical/usd_3my.tsv", "$"),
                    parse_yield("historical/inr_3my.tsv", INR)])
    return df
