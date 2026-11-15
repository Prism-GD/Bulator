# Bulator — PRoot, teclado e primeira release

## Estado honesto do projeto

O aplicativo atual é um gerenciador Android de perfis. Ainda não contém um executável PRoot compatível com Android, um rootfs Ubuntu instalado, um terminal Linux/PTY ou um desktop LXQt. O editor de keymap salva presets, mas ainda não envia eventos para um processo Linux.

## Objetivo do produto: Linux completo

O Bulator não será apenas um terminal nem um lançador de comandos. O objetivo final é oferecer um ambiente Lubuntu ARM64 completo dentro do Android, com área de trabalho gráfica e ferramentas de uso diário. O usuário deverá conseguir abrir a área de trabalho, navegar por arquivos, abrir configurações, usar o terminal, instalar programas e trabalhar com várias janelas.

O desktop planejado inclui LXQt, painel e menu de aplicativos, PCManFM-Qt (gerenciador de arquivos), terminal, configurações do sistema e instalação de programas pelo APT. Integrações Android como teclado virtual/keymap, redimensionamento de tela, clipboard e acesso controlado a arquivos serão desenvolvidas e testadas separadamente. Recursos como áudio e integração adicional com Android serão avaliados conforme compatibilidade.

A entrega será incremental: a alpha valida PRoot e o sistema base; a beta acrescenta e valida o desktop gráfico; a versão 1.0 só será considerada completa quando a experiência de desktop e os componentes essenciais estiverem funcionando. Portanto, a alpha de terminal não representa o produto final.

## Arquitetura alvo

- Aplicativo Android Kotlin (interface, containers, permissões e armazenamento privado).
- PRoot compilado para Android ARM64 a partir do upstream `proot-me/proot`, sem scripts, patches ou dependências do Termux.
- Dependências nativas compiladas/reproduzíveis no GitHub Actions com Android NDK; artefatos e licenças registrados.
- Ubuntu Base 24.04 ARM64 oficial como rootfs inicial; instalar componentes LXQt/Lubuntu por APT depois do primeiro boot.
- Cada container possui rootfs, cache, downloads e configuração próprios.
- Nenhuma execução como root e nenhuma alegação de virtualização de hardware.

## Persistência: os dados precisam continuar lá

Salvar arquivos é requisito central do Bulator, não um extra. Cada container terá armazenamento persistente e independente:

- **Sistema Linux:** rootfs completo, incluindo pacotes instalados e atualizações feitas pelo APT.
- **Pasta pessoal:** `/home/<usuario>`, com documentos, projetos, downloads e arquivos criados.
- **Configurações:** preferências do LXQt, temas, atalhos, configurações de aplicativos e shell.
- **Keymap:** layouts e presets salvos, associados ao usuário/container quando apropriado.
- **Estado do container:** nome, distribuição, resolução, preferências e metadados.

Fechar o Bulator, parar o container ou reiniciar o Android não pode apagar esses dados. Ao iniciar novamente, o Bulator deve montar o mesmo rootfs e restaurar a sessão de trabalho (os programas em execução podem precisar ser reabertos; não prometer restauração automática de processos).

O armazenamento deve ficar em diretório persistente do app, organizado por ID estável do container, e não em uma pasta temporária. Não aplicar quota fixa de 3,9 GB: esse número é uma meta aproximada para o sistema inicial; aplicativos e arquivos do usuário podem ocupar espaço adicional conforme o armazenamento disponível.

### Segurança e recuperação dos dados

- Exibir o espaço usado por cada container e o espaço livre do aparelho.
- Nunca apagar um container ao apenas parar ou fechar o aplicativo.
- Exigir confirmação explícita antes de excluir um container, informando que rootfs e arquivos pessoais serão apagados.
- Planejar exportar/importar ou backup do container para armazenamento escolhido pelo usuário.
- Fazer instalação/atualização do rootfs com estado de progresso e recuperação após interrupção, sem sobrescrever dados pessoais.
- Não expor automaticamente todo o armazenamento Android ao Linux; oferecer pastas compartilhadas com autorização do usuário.
- Documentar que desinstalar o aplicativo pode remover seu armazenamento privado; backup/exportação será necessário para preservar dados fora do aparelho.

## PRoot: etapas obrigatórias

1. Fixar uma tag/commit upstream e registrar a licença GPL-2.0-or-later e avisos de terceiros.
2. Criar workflow de cross-build Android ARM64 com NDK e dependências nativas; não reutilizar builders que partem do repositório Termux.
3. Executar testes de versão, ELF/ABI, loader, ptrace, bind e execução de `/bin/sh` em dispositivo Android real.
4. Empacotar o executável e loader no APK (ou módulo de runtime verificável) e validar instalação/atualização.
5. Baixar rootfs Ubuntu Base por HTTPS, verificar SHA256 publicado, extrair com segurança (bloquear path traversal), e marcar container como pronto somente após validação de `/bin/sh`.
6. Iniciar shell com binds Android estritamente necessários; não expor diretórios pessoais por padrão.

PRoot depende de `ptrace`. A disponibilidade e as restrições do Android variam por versão/fabricante; portanto, build bem-sucedido não prova funcionamento no Samsung A04s. Teste em aparelho é obrigatório.

## Keymap: fluxo funcional

1. Converter cada tecla configurada em um evento Android/Linux bem definido.
2. Manter um modelo de layout (posição, tamanho, tecla, modificadores e preset) independente da interface.
3. Um overlay visual envia eventos DOWN/UP para a camada de terminal focada.
4. O terminal deve possuir uma PTY real ligada ao processo PRoot. Caracteres simples podem ser escritos no stdin, mas setas, Ctrl, Alt, Shift, Esc, Tab e F1–F12 exigem sequências/eventos corretos.
5. Tratar repetição, pressionamento simultâneo, liberação ao perder foco, teclado virtual e rotação de tela.
6. Testes automatizados para tradução de teclas e teste manual com shell real.

**Não considerar o keymap pronto** enquanto não for possível executar comandos e atalhos no shell PRoot usando os botões virtuais.

## Fundo do pinguim

A arte aprovada deve ocupar toda a tela, com escala `centerCrop` e camada escura translúcida por cima para preservar contraste. Botões e cartões ficam acima da imagem. A imagem deve ser incluída como recurso local do APK, não carregada da internet.

## Plano da primeira release

### 0.3.0-alpha — Runtime de teste
- PRoot Android ARM64 compilado e licenciado.
- Diagnóstico de compatibilidade ptrace.
- Instalação/verificação do Ubuntu Base ARM64.
- Shell real interativo via PTY.
- Keymap básico funcional: letras, números, Enter, Backspace, Tab, Esc, setas e modificadores.
- Containers independentes e persistentes, com home, configurações e pacotes preservados após fechar/reiniciar.
- Fundo do pinguim integrado.
- Sem LXQt nesta alpha, a menos que seja estável e validado.

### 0.4.0-beta — Desktop gráfico completo
- Instalação LXQt/Lubuntu via APT.
- Área de trabalho gráfica com painel, menu de aplicativos e janelas.
- PCManFM-Qt, terminal gráfico, configurações do sistema e utilitários essenciais.
- Renderização gráfica por servidor compatível com Android (avaliar e validar a solução).
- Keymap funcional também no desktop, não apenas no terminal.
- Mapeador visual arrastável, redimensionável e presets.
- Medição de armazenamento e recuperação de instalação interrompida.

### 1.0.0 — Lubuntu completo para Android
- Área de trabalho LXQt utilizável e componentes essenciais do Lubuntu integrados.
- Gerenciador de arquivos, terminal gráfico, configurações, menu e instalação de aplicativos pelo APT.
- Keymap integrado ao terminal e ao desktop.
- Instalação limpa e atualização reproduzíveis.
- Testes no Samsung A04s e em outros ARM64.
- Logs exportáveis, diagnóstico e mensagens de erro úteis.
- Verificação de integridade dos downloads e documentação de limitações.
- Publicação de APK assinado e notas de versão.

## Critérios de aprovação da 0.3.0-alpha

- [ ] APK instala no A04s.
- [ ] PRoot ARM64 executa `--version` no Android.
- [ ] PRoot consegue iniciar `/bin/sh` no rootfs Ubuntu.
- [ ] `id`, `uname -m`, `pwd`, `ls` e `apt update` funcionam.
- [ ] Teclas físicas e virtuais chegam ao shell.
- [ ] Dois containers não compartilham rootfs/configuração.
- [ ] Arquivos em /home e pacotes instalados continuam presentes após parar, fechar e reabrir o Bulator.
- [ ] Excluir container exige confirmação explícita; backup/exportação está documentado.
- [ ] Instalação interrompida pode ser retomada sem corromper o rootfs.
- [ ] Fundo do pinguim aparece atrás da interface em diferentes tamanhos de tela.

## Referências técnicas

- PRoot upstream: https://github.com/proot-me/proot
- Manual PRoot: https://proot-me.github.io/
- Ubuntu Base ARM64: https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/
