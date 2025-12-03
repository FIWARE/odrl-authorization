#! /bin/bash

README_FILE=README.md
sed -i.bak '/<!-- BEGIN HELM DOCS -->/,/<!-- END HELM DOCS -->/{
    /<!-- BEGIN HELM DOCS -->/!{
        /<!-- END HELM DOCS -->/!d
    }
}' "$README_FILE"

VALUES="$(helm-docs -t helm-docs/properties.gotmpl --dry-run -l ERROR)"
printf '%s\n' "$VALUES" > temp_values.txt
sed "/<!-- BEGIN HELM DOCS -->/r temp_values.txt" "$README_FILE" > tmp && mv tmp "$README_FILE"
rm temp_values.txt README.md.bak
