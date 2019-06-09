import pandas as pd


def ledger_balance(ledger, level=None):
    """
    returns the balance per commodity per account
    if level is passed, aggregates at `level`
    """
    def subaccount(account):
        if level is None:
            return account
        else:
            splits = account.split(":")[:level]
            return ":".join(splits)

    df = (ledger
          .assign(account=lambda df: df["account"].apply(subaccount))
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
