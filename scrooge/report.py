import seaborn as sns
import scrooge.ledger as sl
import matplotlib.pyplot as plt
from scrooge.constants import EUR
from matplotlib.ticker import FuncFormatter


def plot_portfolio(ledger, prices, threshold=0.01, commodity=EUR):
    """
    returns (figure, axes) for a net worth plot
    """
    f, ax = plt.subplots(1, 1)
    f.set_size_inches(20, 12)
    networth = sl.net_worth(ledger, prices, commodity).loc[commodity]["amount"]
    portfolio = sl.portfolio(ledger, prices)
    data = (portfolio
            .loc[lambda df: df["share"] > threshold]
            .assign(amount=lambda df: df["share"]*networth)
            .sort_values("amount", ascending=False))
    sns.barplot(data=data.reset_index(), x="commodity", y="amount", ax=ax)
    ax.get_yaxis().set_major_formatter(FuncFormatter(lambda x, p: format(int(x), ',')))
    ax.set_xlabel("")
    ax.set_ylabel(f"Amount ({commodity})")
    ax.set_title(f"Net Worth, {commodity} {networth:,.2f}\n"
                 f"(share > {threshold * 100:.2f}%)")

    for (idx, com) in enumerate(data.index):
        plt.text(idx, data.loc[com]["amount"],
                 "{:.2f}%".format(data.loc[com]["share"]*100))

    return (f, ax)


def plot_statement(statement, commodity, from_date=None, to_date=None):
    """
    returns (figure, axes) for a statement plot in a particular `commodity`
    `from_date` and `to_date` are optional and filter the statement
    """
    f, ax = plt.subplots(1, 1)
    f.set_size_inches(20, 12)
    data = (statement
            .reset_index()
            .loc[lambda df: df["commodity"] == commodity]
            .loc[lambda df: (from_date is None) |\
                            (df["date"] >= from_date)]
            .loc[lambda df: (to_date is None) |\
                            (df["date"] < to_date)])
    sns.barplot(data=data, x="date", y="amount", hue="account", ax=ax)
    ax.get_yaxis().set_major_formatter(FuncFormatter(lambda x, p: format(int(x), ',')))
    ax.set_xlabel("")
    ax.set_ylabel(f"Amount ({commodity})")
    ax.set_title(f"Account Statement ({commodity})")
    return (f, ax)
