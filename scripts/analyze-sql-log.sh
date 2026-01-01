#!/bin/bash

# ============================================================================
# SQL Log Analyzer for Tateca Backend
# Usage: tail -f logs/app.log | ./scripts/analyze-sql-log.sh
#    or: ./scripts/analyze-sql-log.sh < logs/app.log
# ============================================================================

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Global counters
TOTAL_QUERIES=0
SELECT_COUNT=0
INSERT_COUNT=0
UPDATE_COUNT=0
DELETE_COUNT=0
JOIN_FETCH_COUNT=0

# Per-endpoint tracking (simple variables, no associative arrays)
CURRENT_ENDPOINT=""
ENDPOINT_QUERY_COUNT=0

print_header() {
    echo -e "\n${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${CYAN}$1${NC}"
    echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

# Main analysis
main() {
    local line
    local query_buffer=""
    local in_query=false
    local in_stats=false
    local stats_buffer=""

    echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║        SQL Log Analyzer - Real-time Monitoring       ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
    echo -e "${YELLOW}Analyzing SQL queries and detecting N+1 problems...${NC}\n"

    while IFS= read -r line; do
        # Detect API endpoint calls
        if [[ $line =~ "GET /"|"POST /"|"PATCH /"|"DELETE /" ]]; then
            local endpoint=$(echo "$line" | grep -oE "(GET|POST|PATCH|DELETE) /[^ ]*" || true)
            if [[ -n "$endpoint" ]] && [[ "$endpoint" != "$CURRENT_ENDPOINT" ]]; then
                # Print summary for previous endpoint
                if [[ -n "$CURRENT_ENDPOINT" ]] && [[ $ENDPOINT_QUERY_COUNT -gt 0 ]]; then
                    echo -e "  ${BOLD}Summary: $ENDPOINT_QUERY_COUNT queries${NC}"
                    if [[ $ENDPOINT_QUERY_COUNT -gt 10 ]]; then
                        echo -e "  ${RED}⚠ WARNING: Possible N+1 problem detected!${NC}"
                    fi
                    echo ""
                fi

                CURRENT_ENDPOINT="$endpoint"
                ENDPOINT_QUERY_COUNT=0
                echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
                echo -e "${BLUE}▶ API Call: ${CYAN}$CURRENT_ENDPOINT${NC}"
            fi
        fi

        # Detect SQL queries (org.hibernate.SQL pattern)
        if [[ $line =~ "org.hibernate.SQL"[[:space:]]*: ]]; then
            in_query=true
            query_buffer=""
            continue
        fi

        if [[ $in_query == true ]]; then
            # Check if this is the start of SQL content
            if [[ $line =~ ^[[:space:]]+(select|insert|update|delete|from|where|join) ]]; then
                query_line=$(echo "$line" | xargs)
                query_buffer="$query_buffer $query_line"

                # Identify query type from first keyword
                if [[ $query_buffer =~ ^[[:space:]]*select|^select ]]; then
                    if [[ $SELECT_COUNT -eq 0 ]] || [[ $(($TOTAL_QUERIES % 10)) -eq 0 ]]; then
                        # Check for JOIN FETCH
                        if [[ $query_buffer =~ "join fetch"|"JOIN FETCH" ]]; then
                            JOIN_FETCH_COUNT=$((JOIN_FETCH_COUNT + 1))
                            echo -e "  ${GREEN}✓ SELECT with JOIN FETCH${NC}"
                        else
                            echo -e "  ${YELLOW}→ SELECT${NC}"
                        fi
                    fi
                    SELECT_COUNT=$((SELECT_COUNT + 1))
                    TOTAL_QUERIES=$((TOTAL_QUERIES + 1))
                    ENDPOINT_QUERY_COUNT=$((ENDPOINT_QUERY_COUNT + 1))
                    in_query=false

                elif [[ $query_buffer =~ ^[[:space:]]*insert|^insert ]]; then
                    echo -e "  ${MAGENTA}→ INSERT${NC}"
                    INSERT_COUNT=$((INSERT_COUNT + 1))
                    TOTAL_QUERIES=$((TOTAL_QUERIES + 1))
                    ENDPOINT_QUERY_COUNT=$((ENDPOINT_QUERY_COUNT + 1))
                    in_query=false

                elif [[ $query_buffer =~ ^[[:space:]]*update|^update ]]; then
                    echo -e "  ${YELLOW}→ UPDATE${NC}"
                    UPDATE_COUNT=$((UPDATE_COUNT + 1))
                    TOTAL_QUERIES=$((TOTAL_QUERIES + 1))
                    ENDPOINT_QUERY_COUNT=$((ENDPOINT_QUERY_COUNT + 1))
                    in_query=false

                elif [[ $query_buffer =~ ^[[:space:]]*delete|^delete ]]; then
                    echo -e "  ${RED}→ DELETE${NC}"
                    DELETE_COUNT=$((DELETE_COUNT + 1))
                    TOTAL_QUERIES=$((TOTAL_QUERIES + 1))
                    ENDPOINT_QUERY_COUNT=$((ENDPOINT_QUERY_COUNT + 1))
                    in_query=false
                fi
            elif [[ ! $line =~ ^[[:space:]] ]]; then
                # End of SQL query
                in_query=false
            fi
        fi

        # Detect and analyze Hibernate statistics (multi-line)
        if [[ $line =~ "StatisticalLoggingSessionEventListener" ]] && [[ $line =~ "Session Metrics" ]]; then
            in_stats=true
            stats_buffer="${line#*: }"
            continue
        fi

        if [[ $in_stats == true ]]; then
            stats_buffer="$stats_buffer $line"

            # Extract JDBC statement count
            if [[ $line =~ ([0-9]+)[[:space:]]+JDBC[[:space:]]+statements ]]; then
                local stmt_count="${BASH_REMATCH[1]}"

                echo -e "\n${CYAN}📊 Hibernate Statistics:${NC}"
                echo -e "  JDBC Statements: ${BOLD}$stmt_count${NC}"

                # Warn about high statement counts
                if [[ $stmt_count -gt 100 ]]; then
                    echo -e "  ${RED}🚨 CRITICAL: $stmt_count statements - Severe N+1 problem!${NC}"
                elif [[ $stmt_count -gt 20 ]]; then
                    echo -e "  ${YELLOW}⚠ WARNING: $stmt_count statements - Possible N+1 problem${NC}"
                elif [[ $stmt_count -gt 0 ]]; then
                    echo -e "  ${GREEN}✓ Normal query count${NC}"
                fi
                echo ""
            fi

            if [[ $line =~ "}" ]]; then
                in_stats=false
                stats_buffer=""
            fi
        fi
    done

    # Print final summary
    if [[ -n "$CURRENT_ENDPOINT" ]] && [[ $ENDPOINT_QUERY_COUNT -gt 0 ]]; then
        echo -e "  ${BOLD}Summary: $ENDPOINT_QUERY_COUNT queries${NC}"
        if [[ $ENDPOINT_QUERY_COUNT -gt 10 ]]; then
            echo -e "  ${RED}⚠ WARNING: Possible N+1 problem detected!${NC}"
        fi
        echo ""
    fi

    print_header "SQL Query Summary"

    echo -e "Total Queries: ${BOLD}${YELLOW}$TOTAL_QUERIES${NC}"
    echo -e ""
    echo -e "Query Breakdown:"
    echo -e "  SELECT:      ${GREEN}$SELECT_COUNT${NC}"
    echo -e "  INSERT:      ${MAGENTA}$INSERT_COUNT${NC}"
    echo -e "  UPDATE:      ${YELLOW}$UPDATE_COUNT${NC}"
    echo -e "  DELETE:      ${RED}$DELETE_COUNT${NC}"
    echo -e ""
    echo -e "Optimization:"
    echo -e "  JOIN FETCH:  ${GREEN}$JOIN_FETCH_COUNT${NC} (prevents N+1 queries)"
    echo -e ""

    # Performance assessment
    if [[ $TOTAL_QUERIES -gt 100 ]]; then
        echo -e "${RED}🚨 CRITICAL: Very high query count ($TOTAL_QUERIES queries)${NC}"
        echo -e "${RED}   This strongly indicates N+1 problem or missing JOIN FETCH${NC}"
    elif [[ $TOTAL_QUERIES -gt 50 ]]; then
        echo -e "${YELLOW}⚠ WARNING: High query count ($TOTAL_QUERIES queries)${NC}"
        echo -e "${YELLOW}   Consider using JOIN FETCH to reduce queries${NC}"
    elif [[ $JOIN_FETCH_COUNT -gt 0 ]]; then
        echo -e "${GREEN}✓ Good performance: Using JOIN FETCH to prevent N+1 queries${NC}"
    else
        echo -e "${GREEN}✓ Normal query count${NC}"
    fi

    echo ""
}

# Run main function
main "$@"
