
PAGES_SRC = $(wildcard *.md)
PAGES = $(addprefix built/,${PAGES_SRC:.md=.html})

all: ${PAGES}

built/%.html: %.md
	markdown $< -f $@

clean:
	rm *.html
