#!/usr/bin/env bash
# Gercek bir banka bildirimini test setine ekler.
#
# Kullanim:
#   ./scripts/add-fixture.sh "Garanti: ... 245,90 TL harcama" EXPENSE 24590 "Migros"
#   ./scripts/add-fixture.sh "Tek kullanimlik sifreniz 1234" IGNORE - -
#
# Ardindan:  gradle :parser:test
set -euo pipefail

if [ "$#" -ne 4 ]; then
  echo "Kullanim: $0 <metin> <EXPENSE|REFUND|IGNORE|NONE> <kurus|-> <isyeri|->" >&2
  exit 1
fi

FIXTURES="$(dirname "$0")/../parser/src/test/resources/fixtures.tsv"
printf '%s\t%s\t%s\t%s\n' "$1" "$2" "$3" "$4" >> "$FIXTURES"
echo "Eklendi. Simdi calistirin:  gradle :parser:test"
