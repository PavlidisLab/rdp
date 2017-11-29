<div>
	<div class="main-header">
		<h2>Help</h2>
		<em>about</em>
	</div>

	<div class="main-content">
		<!-- NAV TABS -->
		<ul class="nav nav-tabs">
			<li class="active"><a href="#about-tab" data-toggle="tab"><i
					class="fa fa-question-circle"></i> User Manual</a></li>
			<li class=""><a href="#faq-tab" data-toggle="tab"><i
					class="fa fa-info-circle"></i> FAQ</a></li>
		</ul>
		<!-- END NAV TABS -->
		<div class="tab-content help-page">
			<div class="tab-pane active" id="about-tab">
				<div class="row">
					<div class="col-md-12">
						<div class="panel-group" id="aboutaccordion" role="tablist"
							aria-multiselectable="true">
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#aboutcollapse1"
									data-parent="#aboutaccordion" aria-expanded="false"
									aria-controls="aboutcollapse1" class="panel-heading" role="tab"
									id="aboutheading1">
									<h4 class="panel-title">
										<a href="#"> Using 'My Account' </a>
									</h4>
								</div>
								<div id="aboutcollapse1" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="aboutheading1">
									<div class="panel-body">
										<div class="panel">
											<a data-toggle="collapse" href="#aboutcollapseProfile">Fill out profile information &#187; </a>
											<div id="aboutcollapseProfile"
												class="panel-collapse collapse">
												<div class="panel-body">
													<p>
														On the <b>Profile</b> tab under "<i>My Account</i>", use
														the edit functions to provide basic information:
													</p>
													<ul>
														<li>Name</li>
														<li>Organization</li>
														<li>Department</li>
													</ul>

													<p>Complete and accurate information is important to
														ensure the Network can contact you.</p>

													<p>You can also provide:</p>
													<ul>
														<li>A link to your home page (e.g., your lab site)</li>
														<li>A phone number</li>
														<li>A summary of your research interests (we
															recommend 100-500 words)</li>
														<li>A list of PubMed IDs for your publications.</li>
													</ul>

													<p>
														These fields are optional. Your email address is not
														editable because it serves as your User ID. From the "<i>My
															Account</i>" page you can also edit your account settings.
														Currently the only editable setting is your password.
													</p>
												</div>
											</div>
										</div>


										<div class="panel">
											<a data-toggle="collapse" href="#aboutcollapsePub">Finding
												PubMed IDs &#187; </a>
											<div id="aboutcollapsePub" class="panel-collapse collapse">
												<div class="panel-body">
													<p>
														To rapidly get a list of your PubMed IDs, in a new browser
														window use PubMed search (<a
															href="http://www.ncbi.nlm.nih.gov/pubmed/"
															target="_blank">http://www.ncbi.nlm.nih.gov/pubmed/</a>)
														to find your publications. The citations are returned 20
														per page by default, but you can change this by clicking
														on "Display settings" and choosing a different setting for
														"Items per page". You can select the publications you wish
														to use by clicking on the checkboxes; to save all your
														citations do not click any check boxes. After making a
														selection click on "Display settings"; select "PMID List"
														and click "Apply". Copy and paste the resulting list of
														numeric IDs into the field in the Profile.
													</p>
													<p>If your search returns a long list of publications
														and you would like to exclude only few of them click on
														"Send to", select Clipboard and click on "Add to
														Clipboard". In the top-right corner there will be a link
														to items in the Clipboard. Within the Clipboard you can
														remove any items you want and then export the PubMed IDs
														as described above.</p>
													<p>
														<b>Note</b>: Currently the PubMed IDs are for future use
														and are optional.
													</p>
													<a href="images/pubmed.png" data-lightbox="pubmed"> <img
														class="thumbnail" src="images/pubmed.png" alt="pubmed">
													</a>
													<!-- <img height="50px" width="50px" src="images/pubmed.png"></img> -->

												</div>
											</div>
										</div>
									</div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#aboutcollapse2"
									data-parent="#aboutaccordion" aria-expanded="false"
									aria-controls="aboutcollapse2" class="panel-heading" role="tab"
									id="aboutheading2">
									<h4 class="panel-title">
										<a href="#"> Using 'My Model Organisms' </a>
									</h4>
								</div>
								<div id="aboutcollapse2" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="aboutheading2">
									<div class="panel-body">

										<div class="panel-group" id="aboutaccordion2-1">
											<div class="panel">
												<a data-toggle="collapse" data-parent="#aboutaccordion2-1"
													href="#aboutcollapse2-1">Add Model Organism &#187; </a>
												<div id="aboutcollapse2-1" class="panel-collapse collapse">
													<div class="panel-body">
														<p>To add a model organism:</p>
														<ol>
															<li>Click the side bar "<i>My Model Organisms</i>"
															</li>
															<li>Click on "<i>Add Organism</i>"
															</li>
															<li>Select the desired organism's name</li>
														</ol>
														<p>
															It will be moved to the top section of "<i>My Model
																Organisms</i>" and the main panel will show you the Manager
															for that organism. You can have multiple Model Organisms
															associated with your account.
														</p>
														<p>Also see relevant Frequently Asked Questions:</p>
														<ul>
														<li><a href="#" onclick='return utility.openAccordian($("#collapse9"))'>What if I study more than one model organism?</a></li>
														<li><a href="#" onclick='return utility.openAccordian($("#collapse10"))'>Are humans a "model" for the purposes of the ${ SETTINGS["rdp.shortname"]} registry?</a></li>
														<li><a href="#" onclick='return utility.openAccordian($("#collapse11"))'>What if the organism I study is not listed?</a></li>
														</ul>
													</div>
												</div>
											</div>
											<div class="panel">
												<a data-toggle="collapse" data-parent="#aboutaccordion2-1"
													href="#aboutcollapse2-2">Model Organism Manager &#187; </a>
												<div id="aboutcollapse2-2" class="panel-collapse collapse">
													<div class="panel-body">
														<p>
															In the manager, you can optionally enter a description of
															your research on this organism (Organism Research Focus), if you feel it is
															different from the description you may have provided in
															the "<i>My Account</i>" section (About My Research).
														</p>

														<p>The most important functionality of the Manager is
															the addition of genes, and then of Gene Ontology terms.</p>
													</div>
												</div>
											</div>
											<div class="panel">
												<a data-toggle="collapse" data-parent="#aboutaccordion2-1"
													href="#aboutcollapse2-3">Adding Genes &#187; </a>
												<div id="aboutcollapse2-3" class="panel-collapse collapse">
													<div class="panel-body">
														<p>The main goal of the registry is to collect an accurate
															and comprehensive list of the genes that you study or are
															able to study. Genes that you are entering directly into
															the system can be either Primary or Secondary
															(non-Primary).</p>
															
															<ul>
															<li><b>Primary genes</b> (Tier 1) are those you currently
																work on in a model organism. You must be able to
																immediately, specifically and rapidly study them in your
																laboratory. Genes you have recently published on are
																especially good Primary genes. We expect this to
																typically be between one and ten genes.</li>
															<li><b>Secondary genes</b> (Tier 2) are those you do not
																mark as Primary. Carefully choose genes which you are
																not necessarily actively investigating but would be able
																to work on rapidly and specifically with minimal set-up
																time. These might be paralogs of Tier 1 genes, or
																members of the same complexes or pathway. For most
																registrants we expect there might be between one and 100
																non-Primary genes entered.</li>
														</ul>
														
														<p>For more information, see the relevant <a href="#" onclick='return utility.openAccordian($("#collapse8"))'>FAQ</a> </p>

														<p>Also see relevant Frequently Asked Questions:</p>
														<ul>
														<li><a href="#" onclick='return utility.openAccordian($("#collapse12"))'>Should I fill in the human orthologs of genes I study?</a></li>
														<li><a href="#" onclick='return utility.openAccordian($("#collapse13"))'>I study the entire genome, or have the ability to assay the function of any gene. How do I select which genes I should list?</a></li>
														</ul>

														<div class="panel">
															<a data-toggle="collapse" href="#aboutcollapseAddGenes">Detailed Instructions &#187; </a>
															<div id="aboutcollapseAddGenes"
																class="panel-collapse collapse">
																<div class="panel-body">
																	<p>To get started, click on "Add Gene(s)".</p>

																	<a href="images/AddGenes1.png"
																		data-lightbox="AddGenes1"> <img class="thumbnail"
																		src="images/AddGenes1.png" alt="AddGenes1">
																	</a>

																	<p>In the pop-up window, use the "Search for Gene
																		field" to search by gene symbol, name or alias. Select
																		the gene and click "Add".</p>

																	<a href="images/AddGenes2.png"
																		data-lightbox="AddGenes2"> <img class="thumbnail"
																		src="images/AddGenes2.png" alt="AddGenes2">
																	</a>

																	<p>Alternatively, you can type or paste multiple
																		genes into the "Bulk Upload" field and click "Add
																		All". You will see a warning if any of your
																		identifiers didn't match a gene in our database.</p>

																	<a href="images/AddGenes3.png"
																		data-lightbox="AddGenes3"> <img class="thumbnail"
																		src="images/AddGenes3.png" alt="AddGenes3">
																	</a>

																	<p>For both Search and Bulk Upload, check the box
																		to indicate if the gene(s) are Primary or not.</p>
																	<p>The added genes will appear in the table at the
																		bottom of the Model Organism Manager. You can remove
																		genes by selecting them and clicking the "Remove
																		Selected" button. You can also change the Primary
																		status of genes one at a time.</p>
																		
																	<a href="images/AddGenes4.png"
																		data-lightbox="AddGenes4"> <img class="thumbnail"
																		src="images/AddGenes4.png" alt="AddGenes4">
																	</a>

																	<p>
																		Finally, be sure to <b>Save Changes</b>. If you
																		navigate away from the page without doing so, you will
																		be prompted to confirm.
																	</p>

																</div>
															</div>
														</div>
													</div>
												</div>
											</div>
											<div class="panel">
												<a data-toggle="collapse" data-parent="#aboutaccordion2-1"
													href="#aboutcollapse2-4">Gene Ontology Terms &#187; </a>
												<div id="aboutcollapse2-4" class="panel-collapse collapse">
													<div class="panel-body">
														<p>
															Once you have added some genes, you can optionally
															provide some Gene Ontology (GO) terms related to your
															work. Genes associated with the GO terms you select will
															be considered a "third Tier" of genes for possible use in
															identifying researchers who can study a particular gene.
															For more details about GO, please see <a
																href="http://geneontology.org/" target="_blank">geneontology.org</a>.
														</p>

														<div class="panel">
															<a data-toggle="collapse" href="#aboutcollapseGO">Detailed Instructions &#187; </a>
															<div id="aboutcollapseGO" class="panel-collapse collapse">
																<div class="panel-body">
																	<p>
																		To enter GO terms for an organism, click on the "Gene
																		Ontology Terms" tab on the Model Organism Manager
																		page. Click on "Add GO Term(s)", which will cause a
																		new popup window to appear. From this window you may
																		manually search for GO terms and/or select suggested
																		terms (you need to have at least two genes saved as
																		Tier 1 and/or Tier 2 genes in order to use GO term
																		suggestion functionality). To read about how the term
																		suggestion tool works, go <a href="#"
																			onclick='return utility.openAccordian($("#aboutcollapse2-5"))'>here</a>.
																	</p>
																	<a href="images/GO1.png" data-lightbox="GO1"> <img
																		class="thumbnail" src="images/GO1.png" alt="GO1">
																	</a>

																	<p></p>

																	<p>Suggested GO terms will appear in a table (allow
																		for a short delay especially if you have a lot of
																		genes saved) that has the following columns:</p>

																	<ul>
																		<li>GO Aspect, which indicates one of three
																			domains from which the GO term originates (biological
																			process (BP), molecular function (MF), cellular
																			component (CC))</li>
																		<li>GO term full name</li>
																		<li>Overlap, which indicates how many of your
																			(Tier 1 and 2) genes are annotated with that term</li>
																		<li>Term Size, which indicates how many genes in
																			the currently chosen model organism are annotated
																			with that term and will be added as Tier 3 genes if
																			the term is selected (excluding the highlighted genes
																			that are already in the system)</li>
																	</ul>

																	<a href="images/GO2.png" data-lightbox="GO2"> <img
																		class="thumbnail" src="images/GO2.png" alt="GO2">
																	</a>

																	<p>Use the "Search for Term" field to search by GO
																		ID, Name or description. Note that terms who have more
																		than 100 associated genes will be greyed out and not
																		available for addition to your profile. After selecting
																		terms click "Add".</p>
																		
																	<a href="images/GOSearch.png" data-lightbox="GOSearch"> <img
																		class="thumbnail" src="images/GOSearch.png" alt="GOSearch">
																	</a>

																	<p>You can expand by clicking on the green arrow in
																		the Term Size column to see the genes that are
																		associated with the listed GO term. Genes that you
																		have manually added (Tier 1 and 2) are highlighted. If
																		you choose to add a selected GO term to you profile,
																		genes that are not highlighted will be associated with
																		your profile as "third Tier" genes.</p>

																	<a href="images/GO3.png" data-lightbox="GO3"> <img
																		class="thumbnail" src="images/GO3.png" alt="GO3">
																	</a>

																	<p>Once you are done, you can close the window and
																		review the GO terms in the Manager. As with genes, you
																		can select and then remove terms, or add more terms
																		later.</p>

																	<p>
																		Once again, don't forget to <b>Save Changes</b>.
																	</p>

																</div>
															</div>
														</div>
													</div>
												</div>
											</div>
											<div class="panel">
												<a data-toggle="collapse" data-parent="#aboutaccordion2-1"
													href="#aboutcollapse2-5">How does the Gene Ontology
													Term suggestion work? &#187; </a>
												<div id="aboutcollapse2-5" class="panel-collapse collapse">
													<div class="panel-body">
														<p>
															If you have already entered at least two genes, the
															system can suggest related GO terms for you. The system
															uses a very simple algorithm to do this. We look at GO
															terms shared by the genes you have entered. If a term has
															enough genes overlapping with your genes, it is
															considered a candidate. We then prune terms that are too
															big, too small or highly redundant with another candidate
															term. The final list of terms is then sorted by overlap size for display.
															The full process is a little more complicated than this,
															as it makes use of the structure of the GO hierarchy and
															several heuristics to provide useful results. <a
																href="mailto:${ SETTINGS["rdp.contact.email"]}">Contact
																us</a> if you want more details.
														</p>
													</div>
												</div>
											</div>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
			<div class="tab-pane" id="faq-tab">
				<div class="row">
					<div class="col-md-12">
						<div class="panel-group" id="accordion" role="tablist"
							aria-multiselectable="true">
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse1"
									data-parent="#accordion" aria-expanded="true"
									aria-controls="collapse1" class="panel-heading" role="tab"
									id="heading1">
									<h4 class="panel-title">
										<a href="#"> What is the registry? </a>
									</h4>
								</div>
								<div id="collapse1" class="panel-collapse collapse in"
									role="tabpanel" aria-labelledby="heading1">
									<div class="panel-body"><b>&lt;Insert Description Here&gt;</b></div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse2"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse2" class="panel-heading" role="tab"
									id="heading2">
									<h4 class="panel-title">
										<a href="#"> What is the purpose of the ${ SETTINGS["rdp.shortname"]} registry? </a>
									</h4>
								</div>
								<div id="collapse2" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading2">
									<div class="panel-body"><b>&lt;Insert Description Here&gt;</b></div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse3"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse3" class="panel-heading" role="tab"
									id="heading3">
									<h4 class="panel-title">
										<a href="#"> Who should register? </a>
									</h4>
								</div>
								<div id="collapse3" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading3">
									<div class="panel-body"><b>&lt;Insert Description Here&gt;</b></div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse4"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse4" class="panel-heading" role="tab"
									id="heading4">
									<h4 class="panel-title">
										<a href="#"> Who will see the information I submit? </a>
									</h4>
								</div>
								<div id="collapse4" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading4">
									<div class="panel-body"><b>&lt;Insert Description Here&gt;</b></div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse5"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse5" class="panel-heading" role="tab"
									id="heading5">
									<h4 class="panel-title">
										<a href="#"> What happens after I fill in my information?
										</a>
									</h4>
								</div>
								<div id="collapse5" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading5">
									<div class="panel-body"><b>&lt;Insert Description Here&gt;</b></div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse6"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse6" class="panel-heading" role="tab"
									id="heading6">
									<h4 class="panel-title">
										<a href="#"> What is the process for making a grant award? </a>
									</h4>
								</div>
								<div id="collapse6" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading6">
									<div class="panel-body"><b>&lt;Insert Description Here&gt;</b></div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse7"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse7" class="panel-heading" role="tab"
									id="heading7">
									<h4 class="panel-title">
										<a href="#"> What model organisms are directly supported
											within the registry? </a>
									</h4>
								</div>
								<div id="collapse7" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading7">
									<div class="panel-body">The model organisms that are
										supported within the registry (by maintaining their most
										current gene annotation) are human, mouse, rat, zebrafish,
										roundworm, fruit fly, yeast and E. coli.</div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse8"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse8" class="panel-heading" role="tab"
									id="heading8">
									<h4 class="panel-title">
										<a href="#"> Which genes should I enter into the registry?
										</a>
									</h4>
								</div>
								<div id="collapse8" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading8">
									<div class="panel-body">
										<p>Key to the success of the Network is our ability to
											match human disease genes to model organism researchers who
											can study those genes. We recognize that for any given human
											gene, there is a limited chance that a model organism
											researcher is actively studying that specific gene.
											Therefore we need to be able to cast a wider net. On the
											other hand, we must strike a balance between restricting the
											genes registered and assuming that every lab can study any
											gene.</p>
										<p>To reach this balance, genes are divided into Tiers:</p>
										<ul>
											<li><b>Tier 1 or Primary genes</b> are those you
												currently work on in a model organism. You must be able to
												immediately, specifically and rapidly study them in your
												laboratory. Genes you have recently published on are
												especially good Primary genes. We expect this to typically
												be between one and ten genes.</li>
											<li><b>Tier 2 genes</b> are those you do not mark as
												Primary. Carefully choose genes which you are not
												necessarily actively investigating but would be able to work
												on rapidly and specifically with minimal set-up time. These
												might be paralogs of Tier 1 genes, or members of the same
												complexes or pathway. For most registrants we expect there
												might be between one and 100 non-Primary genes entered.</li>
											<li><b>Tier 3 genes</b> are not specifically selected by
												the registrant. Instead, we use Gene Ontology (GO) terms
												selected to infer them. By indicating the GO terms most
												related to your work, genes having those annotations will be
												considered as Tier 3 genes. There may be several hundred
												such genes.</li>
										</ul>
										<p>
											See also the question about <a href="#"
												onclick='return utility.openAccordian($("#collapse13"))'>I study the entire genome</a>.
										</p>

										<p>Remember that entering genes into the registry is only
											the first step in the granting process. For a given
											human gene needing a match, we will first look for
											registrants who list an ortholog as a Primary (Tier 1) gene.
											If no match is found, the other Tiers will be searched. If a
											gene you entered comes up and you are invited to apply for a
											grant, you will have to provide evidence of your
											readiness to study that gene. Keep that in mind as you select
											your genes.</p>

									</div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse9"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse9" class="panel-heading" role="tab"
									id="heading9">
									<h4 class="panel-title">
										<a href="#"> What if I study more than one model organism?
										</a>
									</h4>
								</div>
								<div id="collapse9" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading9">
									<div class="panel-body">In the registry you can enter
										information for each model organism separately.</div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse10"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse10" class="panel-heading" role="tab"
									id="heading10">
									<h4 class="panel-title">
										<a href="#"> Are humans a "model" for the purposes of the
											${ SETTINGS["rdp.shortname"]} registry? </a>
									</h4>
								</div>
								<div id="collapse10" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading10">
									<div class="panel-body">Yes, if you have an appropriate
										system to study candidate genes as would be required to
										contribute to the understanding of the identified human
										disease genes. This might include biochemical assays or assays
										based on cell lines. New research on human subjects is not
										considered to be within the scope of ${ SETTINGS["rdp.shortname"]} grant projects.
										Do not register human genes merely because they are orthologs
										of the genes you study in a model organism.</div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse11"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse11" class="panel-heading" role="tab"
									id="heading11">
									<h4 class="panel-title">
										<a href="#"> What if the organism I study is not listed? </a>
									</h4>
								</div>
								<div id="collapse11" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading11">
									<div class="panel-body">The registry was constructed
										assuming that the vast majority of registrants work on one or
										more of the most popular model organisms, but we recognize
										this does not cover all researchers. If your organism is not
										listed, you have two options. One is to let us know and we can
										consider adding it, if technically feasible. Alternatively,
										you can select the most closely related organism and enter the
										orthologs of your genes of interest, and indicate clearly in
										your "Research Focus" under Model Organism tab that this was
										done. Remember that the match will always be to a human gene,
										so as long as we can identify you though a search of the
										registry via an ortholog, you will be considered as a
										potential grant applicant.</div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse12"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse12" class="panel-heading" role="tab"
									id="heading12">
									<h4 class="panel-title">
										<a href="#"> Should I fill in the human orthologs of genes
											I study? </a>
									</h4>
								</div>
								<div id="collapse12" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading12">
									<div class="panel-body">No. Only register genes from the
										model organisms you currently work on. Only register human
										genes if you are working on them. See also <a href="#"
												onclick='return utility.openAccordian($("#collapse8"))'>Which genes should
										I enter into the registry?</a> and <a href="#"
												onclick='return utility.openAccordian($("#collapse10"))'>Are humans a "model"</a>.</div>
								</div>
							</div>
							<div class="panel panel-default">
								<div data-toggle="collapse" data-target="#collapse13"
									data-parent="#accordion" aria-expanded="false"
									aria-controls="collapse13" class="panel-heading" role="tab"
									id="heading13">
									<h4 class="panel-title">
										<a href="#"> I study the entire genome, or have the
											ability to assay the function of any gene. How do I select
											which genes I should list? </a>
									</h4>
								</div>
								<div id="collapse13" class="panel-collapse collapse"
									role="tabpanel" aria-labelledby="heading13">
									<div class="panel-body">We recognize that many labs can
										study any gene. The intention of this program is to identify
										researchers who have an existing history of research in
										orthologs of genes identified in human rare disease studies
										(ideally, who are poised to immediately conduct functional
										analyses that inform a human disease gene that has been
										discovered). You should register the genes that you can
										specifically and rapidly study in your laboratory. See <a href="#"
												onclick='return utility.openAccordian($("#collapse2"))'>What
										is the purpose of the ${ SETTINGS["rdp.shortname"]} registry</a> and <a href="#"
												onclick='return utility.openAccordian($("#collapse8"))'>Which genes should
										I enter into the registry?</a>.</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>

	</div>
	<!-- /main-content -->

</div>