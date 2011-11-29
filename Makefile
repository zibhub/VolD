
PAGES_SRC = $(wildcard *.mkd)
PAGES = $(addprefix built/,${PAGES_SRC:.mkd=.html})

all: ${PAGES}

built/%.html: %.mkd
	markdown $< -f $@

clean:
	rm *.html
