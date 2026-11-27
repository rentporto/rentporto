# Diagrams

PlantUML sources of the diagrams of the project report, with the rendered PNG next to each one. `style.iuml` holds the common style.

To render them again (Java 25 and [PlantUML](https://plantuml.com) 1.2026.10; no Graphviz needed, the diagrams use the `smetana` layout):

```bash
java -Djava.awt.headless=true -jar plantuml.jar -tpng docs/diagrams/*.puml
```
