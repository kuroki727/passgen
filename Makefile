JAVAC := javac
JAVA  := java
SRC   := $(wildcard src/passgen/*.java)
OUT   := out

PREFIX   ?= $(HOME)/.local
BINDIR   := $(DESTDIR)$(PREFIX)/bin
SHAREDIR := $(DESTDIR)$(PREFIX)/share/passgen

.PHONY: all run jar install uninstall clean

all: $(OUT)/passgen/Main.class

$(OUT)/passgen/Main.class: $(SRC)
	@mkdir -p $(OUT)
	$(JAVAC) -d $(OUT) $(SRC)

run: all
	$(JAVA) -cp $(OUT) passgen.Main

jar: all
	jar --create --file passgen.jar --main-class passgen.Main -C $(OUT) .

install: jar
	install -Dm644 passgen.jar $(SHAREDIR)/passgen.jar
	install -Dm755 bin/passgen $(BINDIR)/passgen
	@echo "Installed to $(BINDIR)/passgen -- make sure $(PREFIX)/bin is on your PATH."

uninstall:
	rm -f $(BINDIR)/passgen $(SHAREDIR)/passgen.jar
	-rmdir $(SHAREDIR) 2>/dev/null || true

clean:
	rm -rf $(OUT) passgen.jar
