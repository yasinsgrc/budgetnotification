#!/usr/bin/env bash
# Gercek bir banka bildirimini test setine ekler.
#
# Eklenen satir varsayilan olarak REAL damgalanir; yayin karari (roadmap
# madde 2) yalnizca REAL satirlarin dogruluguna bakar. Sentetik bir satiri
# elle eklemeniz gerekirse 5. argumana SYNTHETIC verin.
#
# Kullanim:
#   ./scripts/add-fixture.sh "Garanti: ... 245,90 TL harcama" EXPENSE 24590 "Migros"
#   ./scripts/add-fixture.sh "Tek kullanimlik sifreniz 1234" IGNORE - -
#   ./scripts/add-fixture.sh "..." EXPENSE 24590 "Migros" SYNTHETIC
#
# Kart numarasini, ad-soyadi ve hesap numarasini MASKELEYIN.
#
# Ardindan:  gradle :parser:test   (veya gradle :parser:verify)
set -euo pipefail

if [ "$#" -lt 4 ] || [ "$#" -gt 5 ]; then
  echo "Kullanim: $0 <metin> <EXPENSE|REFUND|IGNORE|NONE> <kurus|-> <isyeri|-> [REAL|SYNTHETIC]" >&2
  exit 1
fi

ORIGIN="${5:-REAL}"
case "$ORIGIN" in
  REAL|SYNTHETIC) ;;
  *) echo "Gecersiz koken: $ORIGIN (REAL veya SYNTHETIC olmali)" >&2; exit 1 ;;
esac

FIXTURES="$(dirname "$0")/../parser/src/test/resources/fixtures.tsv"

# Ayni metin zaten varsa iki kez sayilmasin - dogruluk oranini sisirir.
if [ -f "$FIXTURES" ] && cut -f1 "$FIXTURES" | grep -Fxq "$1"; then
  echo "Bu metin fixtures.tsv icinde zaten var, eklenmedi." >&2
  exit 1
fi

printf '%s\t%s\t%s\t%s\t%s\n' "$1" "$2" "$3" "$4" "$ORIGIN" >> "$FIXTURES"

REAL_COUNT=$(cut -f5 "$FIXTURES" | grep -cx REAL || true)
echo "Eklendi ($ORIGIN). Gercek ornek sayisi: $REAL_COUNT"
echo "Simdi calistirin:  gradle :parser:test"
