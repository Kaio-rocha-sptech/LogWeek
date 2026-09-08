package school.sptech.logweek_api;


public class Apontamento {
    private Integer id;
    private String emailUsuario;
    private String semana;
    private String titulo;
    private String conteudo;
    private String criadoEm;
    private String atualizadoEm;
    private String inicioEm;
    private String fimEm;

    public String getInicioEm() { return inicioEm; }
    public void setInicioEm(String inicioEm) { this.inicioEm = inicioEm; }
    public String getFimEm() { return fimEm; }
    public void setFimEm(String fimEm) { this.fimEm = fimEm; }
    public String getInicioSemana() { return TempoApontamento.semanaDaNota(this).toString(); }
    public String getDataApontamento() { return TempoApontamento.dataDaNota(this).toString(); }
    public Long getDuracaoMinutos() {
        if (inicioEm == null || fimEm == null) return null;
        return java.time.Duration.between(TempoApontamento.dataHora(inicioEm), TempoApontamento.dataHora(fimEm)).toMinutes();
    }

    public Apontamento() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public void setEmailUsuario(String emailUsuario) {
        this.emailUsuario = emailUsuario;
    }

    public String getSemana() {
        return semana;
    }

    public void setSemana(String semana) {
        this.semana = semana;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(String criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(String atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
