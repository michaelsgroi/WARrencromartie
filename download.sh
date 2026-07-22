#!/usr/bin/env bash
set -e

echo "Which data sources do you want to download?"
echo "  1) BR WAR CSVs"
echo "  2) Lahman CSVs"
echo "  3) Retrosheet gamelogs + Chadwick"
echo "  4) All of the above"
read -p "Enter choice(s) space-separated (e.g. 1 3): " choices

make build

run_br=0; run_lahman=0; run_retro=0
for c in $choices; do
  case $c in
    1) run_br=1 ;;
    2) run_lahman=1 ;;
    3) run_retro=1 ;;
    4) run_br=1; run_lahman=1; run_retro=1 ;;
    *) echo "unknown choice: $c" ;;
  esac
done

[ $run_br     -eq 1 ] && make download-br
[ $run_lahman  -eq 1 ] && make download-lahman
[ $run_retro   -eq 1 ] && make download-retrosheet
true
