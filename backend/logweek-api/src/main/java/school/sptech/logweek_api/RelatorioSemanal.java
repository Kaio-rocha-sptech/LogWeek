package school.sptech.logweek_api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RelatorioSemanal {
    private static final String LINHA = "============================================================\n";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] DIAS = {"SEGUNDA-FEIRA", "TERÇA-FEIRA", "QUARTA-FEIRA", "QUINTA-FEIRA", "SEXTA-FEIRA", "SÁBADO", "DOMINGO"};

    // Remove a formatacao das notas antigas somente na exportacao.
    private static String textoSimples(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replaceAll("(?m)^\\s{0,3}#{1,6}\\s+", "")
                .replaceAll("(?m)^\\s*```[^\\n]*", "")
                .replaceAll("(?m)^\\s*[-*+]\\s+", "")
                .replaceAll("!?\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("<[^>]+>", "")
                .replace("**", "").replace("__", "").replace("`", "").trim();
    }

    public static String gerar(List<Apontamento> todas, LocalDate semana) {
        List<Apontamento> notas = new ArrayList<>();
        long totalMinutos = 0;
        for (Apontamento nota : todas) {
            if (TempoApontamento.semanaDaNota(nota).equals(semana)) {
                notas.add(nota);
                if (nota.getDuracaoMinutos() != null) totalMinutos += nota.getDuracaoMinutos();
            }
        }
        notas.sort(Comparator.comparing((Apontamento nota) -> TempoApontamento.dataDaNota(nota))
                .thenComparing(nota -> nota.getInicioEm() == null ? "" : nota.getInicioEm()).thenComparing(Apontamento::getId));
        String periodo = TempoApontamento.periodo(semana);
        String tempo = String.format("%02dh %02dmin", totalMinutos / 60, totalMinutos % 60);
        StringBuilder texto = new StringBuilder(LINHA + "LOGWEEK | APONTAMENTO SEMANAL\n" + LINHA);
        texto.append("\nPeríodo: ").append(periodo).append("\nTotal de notas: ").append(notas.size())
                .append("\nTempo registrado: ").append(tempo).append("\n");
        String grupoAnterior = "";
        for (Apontamento nota : notas) {
            LocalDate data = TempoApontamento.dataDaNota(nota);
            String grupo = DIAS[data.getDayOfWeek().getValue() - 1] + " | " + data.format(DATA);
            // Alguns registros antigos tem semana e data de criacao divergentes.
            // Conserva a semana informada, sem inventar a data da atividade.
            if (nota.getInicioEm() == null && (data.isBefore(semana) || data.isAfter(semana.plusDays(6)))) {
                grupo = "REGISTROS ANTIGOS | DATA DA ATIVIDADE NÃO REGISTRADA";
            }
            if (!grupo.equals(grupoAnterior)) {
                texto.append("\n------------------------------------------------------------\n\n").append(grupo).append("\n");
                grupoAnterior = grupo;
            }
            if (nota.getInicioEm() == null || nota.getFimEm() == null) {
                texto.append("\n[Horário não registrado]\n");
            } else {
                LocalDateTime inicio = TempoApontamento.dataHora(nota.getInicioEm());
                LocalDateTime fim = TempoApontamento.dataHora(nota.getFimEm());
                texto.append("\n[").append(inicio.format(HORA)).append(" - ").append(fim.format(HORA));
                if (!inicio.toLocalDate().equals(fim.toLocalDate())) {
                    texto.append(" (").append(fim.format(DATA)).append(")");
                }
                texto.append(" | ").append(TempoApontamento.duracao(nota.getDuracaoMinutos())).append("]\n");
            }
            texto.append(nota.getTitulo().trim()).append("\n");
            String observacao = nota.getInicioEm() == null ? textoSimples(nota.getConteudo()) : nota.getConteudo().trim();
            if (!observacao.isEmpty()) {
                texto.append("\n").append(observacao).append("\n");
            }
        }
        texto.append("\n").append(LINHA).append("RESUMO DA SEMANA\n").append(LINHA)
                .append("\nApontamentos: ").append(notas.size()).append("\nTempo registrado: ").append(tempo)
                .append("\nPeríodo: ").append(periodo).append("\n\nGerado automaticamente pelo LogWeek.\n");
        return texto.toString();
    }
}
