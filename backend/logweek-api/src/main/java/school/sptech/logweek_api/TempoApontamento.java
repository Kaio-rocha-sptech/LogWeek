package school.sptech.logweek_api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;

public class TempoApontamento {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public static LocalDateTime agora() {
        return LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).truncatedTo(ChronoUnit.MINUTES);
    }

    public static LocalDateTime dataHora(String valor) {
        return LocalDateTime.parse(valor.replace(' ', 'T')).truncatedTo(ChronoUnit.MINUTES);
    }

    public static LocalDate segundaFeira(LocalDate data) {
        return data.minusDays(data.getDayOfWeek().getValue() - 1);
    }

    public static String periodo(LocalDate segunda) {
        return segunda.format(DATA) + " - " + segunda.plusDays(6).format(DATA);
    }

    // Reconhece tambem as semanas em texto gravadas pela versao anterior.
    public static LocalDate lerSemana(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Semana ausente");
        }
        if (valor.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return segundaFeira(LocalDate.parse(valor));
        }
        if (valor.matches("\\d{2}/\\d{2}/\\d{4} - .*")) {
            return segundaFeira(LocalDate.parse(valor.substring(0, 10), DATA));
        }
        String[] partes = valor.split(" - ");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Semana invalida");
        }
        String[] inicio = partes[0].split(" ");
        String[] fim = partes[1].split(" ");
        String[] meses = {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};
        int mes = 0;
        for (int i = 0; i < meses.length; i++) {
            if (meses[i].equals(inicio[1])) {
                mes = i + 1;
            }
        }
        int ano = Integer.parseInt(inicio.length == 3 ? inicio[2] : fim[fim.length - 1]);
        return segundaFeira(LocalDate.of(ano, mes, Integer.parseInt(inicio[0])));
    }

    public static LocalDate semanaDaNota(Apontamento nota) {
        if (nota.getInicioEm() != null) {
            return segundaFeira(dataHora(nota.getInicioEm()).toLocalDate());
        }
        try {
            return lerSemana(nota.getSemana());
        } catch (RuntimeException erro) {
            return segundaFeira(dataHora(nota.getCriadoEm()).toLocalDate());
        }
    }

    public static LocalDate dataDaNota(Apontamento nota) {
        return dataHora(nota.getInicioEm() == null ? nota.getCriadoEm() : nota.getInicioEm()).toLocalDate();
    }

    public static String duracao(long minutos) {
        if (minutos < 60) {
            return minutos + " min";
        }
        return minutos / 60 + "h " + String.format("%02d", minutos % 60) + "min";
    }
}
