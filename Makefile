
PAGES_SRC = $(wildcard *.mkd)
PAGES = ${PAGES_SRC:.mkd=.html}

all: ${PAGES}

%.html: %.mkd
	markdown $< -f $@

clean:
	rm *.html
