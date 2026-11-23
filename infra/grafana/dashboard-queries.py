"""Prints, one per line and URL-encoded, the Prometheus queries of the panels of a Grafana dashboard.

The variables of the dashboard become values Prometheus accepts, so that a script can ask Prometheus to evaluate each
query and fail on the ones it rejects.

Usage: python3 infra/grafana/dashboard-queries.py infra/grafana/dashboards/rentporto.json
"""
import json
import sys
import urllib.parse

VARIABLES = {"$application": ".+", "$__rate_interval": "1m", "$__range": "1h"}


def queries(panels):
    for panel in panels:
        yield from queries(panel.get("panels", []))
        for target in panel.get("targets", []):
            expression = target["expr"]
            for variable, value in VARIABLES.items():
                expression = expression.replace(variable, value)
            yield expression


with open(sys.argv[1]) as dashboard:
    for query in queries(json.load(dashboard)["panels"]):
        print(urllib.parse.quote(query, safe=""))
