# AI Coding Agent Dispatcher

**CRITICAL RULES:**
1. **NO BLIND GREPPING:** Do not use `grep` blindly to find files or code locations.
2. **USE THE MAPS:** Always consult the following JSON maps to locate domains and types:
   - **Domains:** `.agents/maps/domain_index.json` (maps features/domains to file paths)
   - **Types/Models:** `.agents/maps/types_index.json` (maps types, classes, and models to exact line numbers)
3. **SCHEMA/TYPE MODIFICATIONS:** If you modify schemas/types, you MUST run `npm run update-maps` before finishing.

**Historical Architecture Notes:**
If you need historical architecture notes, read `docs/ARCHITECTURE_HISTORY.md`. DO NOT modify the architecture history.
