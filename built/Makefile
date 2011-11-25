
PAGES_SRC = $(wildcard ../*.mkd)
PAGES = $(notdir ${PAGES_SRC:.mkd=.html})

all: ${PAGES}

%.html: ../%.mkd
	markdown $< -f $@

clean:
	rm *.html
