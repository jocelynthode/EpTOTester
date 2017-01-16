SRC = main
READY = main_ready

pdf: 
	$(MAKE) single
	$(MAKE) single
	$(MAKE) single

single: $(SRC).tex
	pdflatex  $<

bib:
	$(MAKE) single
	bibtex $(SRC)
	$(MAKE) pdf

check:
	aspell --lang=pt -c -t *.tex

deliver: bib
	thumbpdf $(SRC)
	$(MAKE) bib
	pdfopt $(SRC).pdf $(READY).pdf

deliverIEEE:
	latex $(SRC).tex
	bibtex $(SRC)
	latex $(SRC).tex
	latex $(SRC).tex
	latex $(SRC).tex
	dvips $(SRC).dvi
	ps2pdf14 -dPDFSETTINGS=/prepress -dEmbedAllFonts=true $(SRC).ps


clean:
	rm -f *.toc *.aux *.log *.ind *.idx *.bak *.lof *.lot *.out *~ *.bbl *.blg *.tpt *.maf *.mtc* 

distclean: clean
	rm -f $(SRC).pdf $(READY).pdf
