import pandas as pd
import scrooge.frame as sf
from datetime import date, timedelta


def ledger_balance(ledger, level=None):
    """
    returns the balance per commodity per account
    if level is passed, aggregates at `level`
    """
    leveled = ledger if level is None else sf.subaccount(ledger, level)
    df = (leveled
          .groupby(["account", "commodity"])
          [["amount"]]
          .sum()
          .reset_index(level=1))
    return df


def convert_balance(balance, prices, commodity):
    """
    converts amounts into commodity units
    """
    divisor = prices.loc[commodity]["value"]
    df = (balance
          .set_index("commodity", append=True)
          .join(prices)
          .assign(amount=lambda df: df["amount"]*df["value"]/divisor)
          .groupby("account")
          [["amount"]]
          .sum()
          .assign(commodity=commodity)
          [["commodity", "amount"]])
    return df


def net_commodities(ledger):
    """
    returns a dataframe of (commodity, amount) representing
    Assets - Liabilities
    """
    balance = ledger_balance(ledger, level=1)
    assets = balance.loc["Assets"]
    liabilities = balance.loc["Liabilities"]
    df = (assets
          .merge(liabilities, on="commodity", how="outer")
          .fillna(0.0)
          .assign(amount=lambda df: df["amount_x"] + df["amount_y"])
          .set_index("commodity")
          [["amount"]])
    return df


def net_worth(ledger, prices, commodity):
    """
    returns a dataframe of (commodity, amount) representing
    Assets - Liabilities in `commodity` units
    """
    divisor = prices.loc[commodity]["value"]
    netcom = net_commodities(ledger)
    worth = (netcom
             .join(prices)
             .assign(amount=lambda df: df["amount"]*df["value"]/divisor)
             ["amount"]
             .sum())
    return (pd.DataFrame([[commodity, worth]], columns=["commodity", "amount"])
            .set_index("commodity"))


def portfolio(ledger, prices):
    """
    returns a dataframe of (commodity, share) representing
    Assets - Liabilities in fractional share of net worth
    """
    netcom = net_commodities(ledger)
    df = (netcom
          .join(prices)
          .assign(amount=lambda df: df["amount"]*df["value"])
          .assign(share=lambda df: df["amount"]/df["amount"].sum())
          [["share"]])
    return df


def daily_statement(ledger, level=None):
    """
    returns (date, account, commodity)
    if level is passed, aggregates at `level`
    """
    leveled = ledger if level is None else sf.subaccount(ledger, level)
    df = (leveled
          .groupby(["date", "account", "commodity"])
          [["amount"]]
          .sum()
          .reset_index(level=[1, 2]))
    return df


def weekly_statement(ledger, level=None):
    """
    returns (date, account, commodity) where date is set
    to the Monday of that week for all dates.
    if level is passed, aggregates at `level`
    """
    def reset_date(d):
        return d - timedelta(days=d.weekday())

    df = (ledger
          .assign(date=lambda df: df["date"].apply(reset_date)))
    return daily_statement(df, level)


def monthly_statement(ledger, level=None):
    """
    returns (date, account, commodity) where date is set
    to the first date of the month for all dates.
    if level is passed, aggregates at `level`
    """
    def reset_date(d):
        return date(d.year, d.month, 1)

    df = (ledger
          .assign(date=lambda df: df["date"].apply(reset_date)))
    return daily_statement(df, level)


def annual_statement(ledger, level=None):
    """
    returns (date, account, commodity) where date is set
    to the first date of the year for all dates.
    if level is passed, aggregates at `level`
    """
    def reset_date(d):
        return date(d.year, 1, 1)

    df = (ledger
          .assign(date=lambda df: df["date"].apply(reset_date)))
    return daily_statement(df, level)
