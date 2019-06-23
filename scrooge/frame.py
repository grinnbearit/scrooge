import pandas as pd


def subaccount(dataframe, level):
    """
    returns a new data frame with the `account` column
    set by `level`
    """
    def split_account(account):
        splits = account.split(":")[:level]
        return ":".join(splits)

    df = (dataframe
          .assign(account=lambda df: df["account"].apply(split_account)))
    return df
