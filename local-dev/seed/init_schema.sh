#!/bin/bash
cat 0.schema.sql  | docker  exec -i kiro-db psql -U admin -d kirodb
