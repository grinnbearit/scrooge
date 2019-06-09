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
