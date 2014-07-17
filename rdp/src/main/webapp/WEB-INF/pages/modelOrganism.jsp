<div class="container">
    <form id="modelOrganism" class="form-horizontal" role="form">

        <div class="form-group">
            <label class="col-sm-3 control-label">Organism</label>
            <div class="col-sm-6">
                <select class="form-control">
                    <option>Mouse</option>
                    <option>Rat</option>
                    <option>Fly</option>
                    <option>Worm</option>
                    <option>Monkey</option>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">About</label>
            <div class="col-sm-6">
                <textarea class="form-control" rows="3"
                    placeholder="My lab studies ..."></textarea>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">Bulk upload</label>
            <div class="col-sm-6">
                <input type="file" id="bulkUploadInputFile">
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">Genes</label>
            <div class="col-sm-6">
                <select id="geneSelect" class="select2"
                    multiple="multiple" style="width: 100%;">
                    <option>BRCA1</option>
                    <option>APOE</option>
                    <option>SNCA</option>
                    <option>CAMK2A</option>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">Pathways /
                processes</label>
            <div class="col-sm-6">
                <select id="pathwaySelect" class="select2"
                    multiple="multiple" style="width: 100%;">
                    <option>Synapse formation</option>
                    <option>Cell signalling</option>
                    <option>Inflamatory response</option>
                    <option>Cell division</option>
                </select>
            </div>
        </div>

        <div class="form-group">
            <label class="col-sm-3 control-label">Diseases /
                phenotypes</label>
            <div class="col-sm-6">
                <select id="phenoSelect" class="select2"
                    multiple="multiple" style="width: 100%;">
                    <option>microcephaly</option>
                    <option>autism</option>
                    <option>cancer</option>
                </select>
            </div>
        </div>

        <button type="submit" class="btn btn-default col-sm-offset-3">Save</button>
    </form>
</div>


<!-- Our scripts -->
<script src="scripts/api/modelOrganism.js"></script>