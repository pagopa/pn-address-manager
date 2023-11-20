# TEST VERIFICA NORMALIZZAZIONE INDIRIZZI

1. LANCIARE TEST:
   ```bash
   ./test.sh Check_List_PagoPA_v5_1_Input.csv <prefix_correlationId>
   ```
2. LANCIARE LO SCRIPT NODE PER LA VERIFICA DEL TEST:
   ```bash
   cd VerificaNormalizzazione
   node  index2.js sso_pn-confinfo-test <prefix_correlationId> ../Check_List_PagoPA_v5_1_Atteso.csv
   ```