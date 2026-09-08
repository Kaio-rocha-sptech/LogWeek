package school.sptech.logweek_api;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TempoApontamentoTests {
    @Test void semanasAtuaisEAntigasConvergemParaSegundaFeira() {
        LocalDate segunda = LocalDate.of(2026, 12, 28);
        assertEquals(segunda, TempoApontamento.lerSemana("2027-01-03"));
        assertEquals(segunda, TempoApontamento.lerSemana("28/12/2026 - 03/01/2027"));
        assertEquals(segunda, TempoApontamento.lerSemana("28 dez 2026 - 3 jan 2027"));
        assertEquals("28/12/2026 - 03/01/2027", TempoApontamento.periodo(segunda));
    }

    @Test void rejeitaDataInexistenteEmVezDeExportarOutraSemana() {
        assertThrows(RuntimeException.class, () -> TempoApontamento.lerSemana("31/02/2026 - 08/03/2026"));
    }
}
