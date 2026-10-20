#!/usr/bin/env bash
# Turn ad-blocking Private DNS (DNS-over-TLS) on or off, with a safety check.
# Some networks (campus, hotel, some ISPs) block port 853. Private DNS then fails every lookup and
# nothing loads, so `on` tests name resolution and switches it straight back off if it fails.
# Note: this blocks ad and tracker domains, not YouTube ads (they come from the same servers as video).
#
# Usage: tools/private-dns.sh on [hostname]   default hostname: dns.adguard-dns.com
#        tools/private-dns.sh off             back to Automatic (opportunistic)
#        tools/private-dns.sh status
set -euo pipefail
# shellcheck source=tools/lib.sh
. "$(dirname "$0")/lib.sh"
require_device

resolves() {
  local out
  out="$(ash ping -c 1 -W 3 "$1" 2>&1 || true)"
  grep -q '^PING' <<< "$out"
}

case "${1:-status}" in
  on)
    host="${2:-dns.adguard-dns.com}"
    [[ $host =~ ^[A-Za-z0-9.-]+$ ]] || die "bad hostname: $host"
    ash settings put global private_dns_specifier "$host"
    ash settings put global private_dns_mode hostname
    ok=""
    for _ in 1 2 3 4 5 6; do
      sleep 3
      if resolves www.google.com; then ok=1; break; fi
    done
    if [ -z "$ok" ]; then
      ash settings put global private_dns_mode opportunistic
      ash settings delete global private_dns_specifier >/dev/null
      die "nothing resolved through $host (this network probably blocks port 853); switched back to Automatic"
    fi
    if resolves doubleclick.net; then
      echo "Private DNS on ($host), but doubleclick.net still resolves: is $host an ad-blocking resolver?"
    else
      echo "Private DNS on ($host): normal sites resolve, doubleclick.net is blocked"
    fi ;;
  off)
    ash settings put global private_dns_mode opportunistic
    ash settings delete global private_dns_specifier >/dev/null
    echo "Private DNS back to Automatic" ;;
  status)
    echo "mode=$(ash settings get global private_dns_mode) host=$(ash settings get global private_dns_specifier)" ;;
  *) die "usage: tools/private-dns.sh on [hostname] | off | status" ;;
esac
